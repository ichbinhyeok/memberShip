package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.domain.log.mybatis.MembershipLogMapper;
import org.example.membership.domain.order.mybatis.OrderMapper;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.mybatis.UserMapper;
import org.example.membership.dto.UserOrderTotal;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyBatisMembershipRenewalService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final MembershipLogMapper membershipLogMapper;
    private final SqlSessionFactory sqlSessionFactory;

    private static final int BATCH_SIZE = 1000;

    /**
     * ✅ 일반 MyBatis 처리 (단건 update + insert 방식)
     */
    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start();

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)
        );

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userMapper.update(user);

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
            log.setChangedAt(LocalDateTime.now());

            membershipLogMapper.insert(log);
        }

        watch.stop();
        log.info("✅ MyBatis 등급 갱신 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                users.size(), watch.getTotalTimeMillis());
    }

    /**
     * ✅ 배치 모드 처리 (ExecutorType.BATCH + flushStatements)
     */
    public void renewMembershipLevelBatch(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start();

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)
        );

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();

        try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            UserMapper batchUserMapper = batchSession.getMapper(UserMapper.class);
            MembershipLogMapper batchLogMapper = batchSession.getMapper(MembershipLogMapper.class);

            int processed = 0;
            for (User user : users) {
                Long userId = user.getId();
                BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

                MembershipLevel oldLevel = user.getMembershipLevel();
                MembershipLevel newLevel = calculateLevel(totalAmount);

                user.setMembershipLevel(newLevel);
                user.setLastMembershipChange(LocalDateTime.now());
                batchUserMapper.update(user);

                MembershipLog log = new MembershipLog();
                log.setUser(user);
                log.setPreviousLevel(oldLevel);
                log.setNewLevel(newLevel);
                log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
                log.setChangedAt(LocalDateTime.now());
                batchLogMapper.insert(log);

                processed++;

                if (processed % BATCH_SIZE == 0) {
                    batchSession.flushStatements();
                    batchSession.commit();
                    batchSession.clearCache();
                }
            }

            // 마지막 flush
            batchSession.flushStatements();
            batchSession.commit();
        }

        watch.stop();
        log.info("✅ MyBatis (배치) 등급 갱신 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                users.size(), watch.getTotalTimeMillis());
    }

    /**
     * 등급 계산 로직
     */
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

    /**
     * 등급 변경 사유 메시지 생성
     */
    private String getReason(MembershipLevel oldLevel, MembershipLevel newLevel, BigDecimal amount) {
        if (newLevel.ordinal() > oldLevel.ordinal()) {
            return "자동 강등 (전월 주문합계: " + amount + ")";
        } else if (newLevel.ordinal() < oldLevel.ordinal()) {
            return "자동 승급 (전월 주문합계: " + amount + ")";
        } else {
            return "등급 유지 (전월 주문합계: " + amount + ")";
        }
    }
}
