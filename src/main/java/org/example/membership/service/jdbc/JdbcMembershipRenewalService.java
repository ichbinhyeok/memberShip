package org.example.membership.service.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.domain.log.jdbc.JdbcMembershipLogRepository;
import org.example.membership.domain.user.User;
import org.example.membership.dto.MembershipLogDto;
import org.example.membership.dto.UserDto;
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
            "SELECT user_id, SUM(order_amount) AS totalAmount FROM orders WHERE ordered_at BETWEEN ? AND ? GROUP BY user_id";

    private static final String UPDATE_USER_LEVEL_SQL =
            "UPDATE users SET membership_level = ?, last_membership_change = ? WHERE id = ?";

    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start();

        LocalDateTime now = LocalDateTime.now();
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        // 1. 주문 총합 집계
        Map<Long, BigDecimal> totalAmountByUser = jdbcTemplate.query(SUM_ORDERS_SQL, rs -> {
            Map<Long, BigDecimal> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("user_id"), rs.getBigDecimal("totalAmount"));
            }
            return map;
        }, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        // 2. 유저 전체 조회
        List<UserDto> users = jdbcTemplate.query(SELECT_ALL_USERS, (rs, rowNum) -> {
            UserDto dto = new UserDto();
            dto.id = rs.getLong("id");
            dto.name = rs.getString("name");
            dto.membershipLevel = rs.getString("membership_level");
            Timestamp changed = rs.getTimestamp("last_membership_change");
            dto.lastMembershipChange = changed != null ? changed.toLocalDateTime() : null;
            Timestamp created = rs.getTimestamp("created_at");
            dto.createdAt = created != null ? created.toLocalDateTime() : null;
            return dto;
        });

        List<UserDto> updatedUsers = new ArrayList<>();
        List<MembershipLogDto> logs = new ArrayList<>();

        for (UserDto user : users) {
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(user.id, BigDecimal.ZERO);
            String oldLevel = user.membershipLevel;
            String newLevel = calculateLevel(totalAmount);

            user.membershipLevel = newLevel;
            user.lastMembershipChange = now;
            updatedUsers.add(user);

            MembershipLogDto log = new MembershipLogDto();
            log.userId = user.id;
            log.previousLevel = oldLevel;
            log.newLevel = newLevel;
            log.changeReason = getReason(oldLevel, newLevel, totalAmount);
            log.changedAt = now;
            logs.add(log);
        }

        // 3. User update → batchUpdate
        jdbcTemplate.batchUpdate(
                UPDATE_USER_LEVEL_SQL,
                updatedUsers,
                BATCH_SIZE,
                (ps, user) -> {
                    ps.setString(1, user.membershipLevel);
                    ps.setTimestamp(2, Timestamp.valueOf(user.lastMembershipChange));
                    ps.setLong(3, user.id);
                }
        );

        // 4. Log insert → batchInsert
        membershipLogRepository.batchInsert(logs);

        watch.stop();
        log.info("✅ JDBC 리팩토링 갱신 완료 - 유저 수: {}, 소요 시간: {}ms", users.size(), watch.getTotalTimeMillis());
    }

    private String calculateLevel(BigDecimal totalAmount) {
        if (totalAmount.compareTo(new BigDecimal("1000000")) >= 0) return "VIP";
        if (totalAmount.compareTo(new BigDecimal("500000")) >= 0) return "GOLD";
        if (totalAmount.compareTo(new BigDecimal("100000")) >= 0) return "SILVER";
        return "SILVER";
    }

    private String getReason(String oldLevel, String newLevel, BigDecimal totalAmount) {
        if (!oldLevel.equals(newLevel)) {
            return (newLevel.compareTo(oldLevel) > 0 ? "자동 강등" : "자동 승급") + " (전월 주문합계: " + totalAmount + ")";
        } else {
            return "등급 유지 (전월 주문합계: " + totalAmount + ")";
        }
    }
}
