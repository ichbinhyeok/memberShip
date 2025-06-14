package org.example.membership.service.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.domain.log.jdbc.JdbcMembershipLogRepository;
import org.example.membership.domain.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdbcMembershipRenewalService {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcMembershipLogRepository membershipLogRepository;

    private static final int BATCH_SIZE = 1000;

    private static final String SELECT_ALL_USERS =
            "SELECT id, name, membership_level, last_membership_change, created_at FROM users";

    private static final String SUM_ORDERS_SQL =
            "SELECT user_id, SUM(order_amount) AS totalAmount " +
                    "FROM orders WHERE ordered_at BETWEEN ? AND ? GROUP BY user_id";

    private static final String UPDATE_USER_LEVEL_SQL =
            "UPDATE users SET membership_level = ?, last_membership_change = ? WHERE id = ?";

    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start();

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        Map<Long, BigDecimal> totalAmountByUser = jdbcTemplate.query(SUM_ORDERS_SQL, rs -> {
            Map<Long, BigDecimal> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("user_id"), rs.getBigDecimal("totalAmount"));
            }
            return map;
        }, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        List<User> users = jdbcTemplate.query(SELECT_ALL_USERS, new UserRowMapper());

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLog> logs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(now);
            updatedUsers.add(user);

            String reason;
            if (newLevel.ordinal() > oldLevel.ordinal()) {
                reason = "자동 강등 (전월 주문합계: " + totalAmount + ")";
            } else if (newLevel.ordinal() < oldLevel.ordinal()) {
                reason = "자동 승급 (전월 주문합계: " + totalAmount + ")";
            } else {
                reason = "등급 유지 (전월 주문합계: " + totalAmount + ")";
            }

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(reason);
            log.setChangedAt(now);
            logs.add(log);
        }

        jdbcTemplate.batchUpdate(
                UPDATE_USER_LEVEL_SQL,
                updatedUsers,
                BATCH_SIZE,
                (ps, user) -> {
                    ps.setString(1, user.getMembershipLevel().name());
                    ps.setTimestamp(2, Timestamp.valueOf(user.getLastMembershipChange()));
                    ps.setLong(3, user.getId());
                }
        );

        membershipLogRepository.batchInsert(logs);

        watch.stop();
        log.info("✅ JDBC 등급 갱신 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                updatedUsers.size(), watch.getTotalTimeMillis());
    }

    private MembershipLevel calculateLevel(BigDecimal totalAmount) {
        if (totalAmount.compareTo(new BigDecimal("1000000")) >= 0) {
            return MembershipLevel.VIP;
        } else if (totalAmount.compareTo(new BigDecimal("500000")) >= 0) {
            return MembershipLevel.GOLD;
        } else if (totalAmount.compareTo(new BigDecimal("100000")) >= 0) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.SILVER;
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setMembershipLevel(MembershipLevel.valueOf(rs.getString("membership_level")));
            Timestamp lastChange = rs.getTimestamp("last_membership_change");
            if (lastChange != null) {
                user.setLastMembershipChange(lastChange.toLocalDateTime());
            }
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                user.setCreatedAt(created.toLocalDateTime());
            }
            return user;
        }
    }
}