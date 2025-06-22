package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.entity.Badge;
import org.example.membership.entity.MembershipLog;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.CategoryRepository;
import org.example.membership.repository.mybatis.MembershipLogMapper;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.UserMapper;
import org.example.membership.dto.MembershipLogRequest;
import org.example.membership.dto.UserOrderTotal;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final CategoryRepository categoryRepository;
    private final BadgeRepository badgeRepository;

    private static final int BATCH_SIZE = 5000;

    /**
     * ✅ 일반 MyBatis 처리 (단간 update + insert 방식)
     */
    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start();

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);
        List<UserCategoryOrderStats> aggregates = orderMapper.aggregateByUserAndCategoryBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)
        );

        class Stats { long count; BigDecimal amount; Stats(long c, BigDecimal a){count=c; amount=a;} }
        Map<Long, Map<Long, Stats>> statMap = new java.util.HashMap<>();
        for (org.example.membership.dto.UserCategoryOrderStats row : aggregates) {
            statMap.computeIfAbsent(row.getUserId(), k -> new java.util.HashMap<>())
                    .put(row.getCategoryId(), new Stats(row.getOrderCount(), row.getTotalAmount()));
        }
        List<User> users = userMapper.findAll();

        for (User user : users) {
            Map<Long, Stats> userStats = statMap.getOrDefault(user.getId(), java.util.Collections.emptyMap());
            long existingBadgeCount = badgeRepository.countByUser(user);
            int newBadgeCount = 0;
            for (Map.Entry<Long, Stats> e : userStats.entrySet()) {
                Stats s = e.getValue();
                if (s.count >= 10 && s.amount.compareTo(new BigDecimal("300000")) >= 0) {
                    var category = categoryRepository.getReferenceById(e.getKey());
                    if (!badgeRepository.existsByUserAndCategory(user, category)) {
                        Badge badge = new Badge();
                        badge.setUser(user);
                        badge.setCategory(category);
                        badge.setAwardedAt(LocalDateTime.now());
                        badgeRepository.save(badge);
                        newBadgeCount++;
                    }
                }
            }
            long badgeCount = existingBadgeCount + newBadgeCount;


            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(badgeCount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userMapper.update(user);

            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason("badge count: " + badgeCount);
            log.setChangedAt(LocalDateTime.now());

            membershipLogMapper.insert(log);
        }

        watch.stop();
        log.info("✅ MyBatis 등급 갱신 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                users.size(), watch.getTotalTimeMillis());
    }

    public void renewMembershipLevelForeach(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();
        List<MembershipLogRequest> logList = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            userMapper.update(user);

            MembershipLogRequest log = new MembershipLogRequest();
            log.setUserId(userId);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
            log.setChangedAt(LocalDateTime.now());
            logList.add(log);
        }

        membershipLogMapper.bulkInsertRequests(logList);
    }

    public void renewMembershipLevelExecutorBatch(LocalDate targetDate) {
        int count = 0;

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLogRequest> logs = new ArrayList<>();

        // 1. 등급 변경 및 로그 생성
        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            updatedUsers.add(user);

            MembershipLogRequest log = new MembershipLogRequest();
            log.setUserId(userId);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
            log.setChangedAt(LocalDateTime.now());

            logs.add(log);
        }

        // 2. 배치 실행
        try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            UserMapper batchUserMapper = batchSession.getMapper(UserMapper.class);
            MembershipLogMapper batchLogMapper = batchSession.getMapper(MembershipLogMapper.class);

            // 2-1. User update batch
            count = 0;
            for (User user : updatedUsers) {
                batchUserMapper.update(user);
                count++;
                if (count % BATCH_SIZE == 0) {
                    batchSession.flushStatements();
                }
            }
            batchSession.flushStatements(); // 마지막 flush

            // 2-2. Log insert batch
            count = 0;
            for (MembershipLogRequest log : logs) {
                batchLogMapper.insertOneRequest(log);
                count++;
                if (count % BATCH_SIZE == 0) {
                    batchSession.flushStatements();
                }
            }
            batchSession.flushStatements(); // 마지막 flush

            batchSession.commit();
        }
    }



    private void insertMembershipLogsInBatch(SqlSession batchSession, List<MembershipLog> logs) {
        if (logs == null || logs.isEmpty()) return;

        MembershipLogMapper logMapper = batchSession.getMapper(MembershipLogMapper.class);

        final int SUB_BATCH_SIZE = 500;
        List<MembershipLog> buffer = new ArrayList<>();

        for (MembershipLog log : logs) {
            buffer.add(log);
            if (buffer.size() == SUB_BATCH_SIZE) {
                logMapper.bulkInsertLogs(buffer);
                buffer.clear();
            }
        }

        if (!buffer.isEmpty()) {
            logMapper.bulkInsertLogs(buffer);
        }
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
            return "자동 강등 (전원 주문합계: " + amount + ")";
        } else if (newLevel.ordinal() < oldLevel.ordinal()) {
            return "자동 승급 (전원 주문합계: " + amount + ")";
        } else {
            return "등급 유지 (전원 주문합계: " + amount + ")";
        }
    }

    public void renewMembershipLevelExecutorBatchWithBulkInsert(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLogRequest> logs = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            updatedUsers.add(user);

            MembershipLogRequest log = new MembershipLogRequest();
            log.setUserId(userId);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
            log.setChangedAt(LocalDateTime.now());

            logs.add(log);
        }

        // 1. update는 ExecutorType.BATCH 사용
        try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            UserMapper batchUserMapper = batchSession.getMapper(UserMapper.class);

            int count = 0;
            for (User user : updatedUsers) {
                batchUserMapper.update(user);
                if (++count % BATCH_SIZE == 0) {
                    batchSession.flushStatements();
                }
            }
            batchSession.flushStatements();
            batchSession.commit();
        }

        // 2. insert는 bulk foreach 사용
        membershipLogMapper.bulkInsertRequests(logs);
    }


    public void renewMembershipLevelCaseWhenInsertForeach(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<UserOrderTotal> aggregates = orderMapper.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(UserOrderTotal::getUserId, UserOrderTotal::getTotalAmount));

        List<User> users = userMapper.findAll();

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLogRequest> logs = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            updatedUsers.add(user);

            MembershipLogRequest log = new MembershipLogRequest();
            log.setUserId(userId);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(getReason(oldLevel, newLevel, totalAmount));
            log.setChangedAt(LocalDateTime.now());

            logs.add(log);
        }

        // 1. update: case when 방식으로 1줄로 묶기
        userMapper.bulkUpdateMembershipLevels(updatedUsers);

        // 2. insert: foreach multi-row SQL
        membershipLogMapper.bulkInsertRequests(logs);
    }

    private MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) {
            return MembershipLevel.VIP;
        } else if (badgeCount == 2) {
            return MembershipLevel.GOLD;
        } else if (badgeCount == 1) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.NONE;
    }

}
