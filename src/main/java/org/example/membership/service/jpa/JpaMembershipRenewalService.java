package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.domain.log.jpa.MembershipLogRepository;
import org.example.membership.domain.log.mybatis.MembershipLogMapper;
import org.example.membership.domain.order.jpa.OrderRepository;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.jpa.UserRepository;
import org.example.membership.dto.MembershipLogRequest;
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
public class JpaMembershipRenewalService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MembershipLogRepository membershipLogRepository;
    private final MembershipLogMapper membershipLogMapper;

    public void renewMembershipLevel(LocalDate targetDate) {
        StopWatch watch = new StopWatch();
        watch.start(); // 시작 시간 기록

        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1); // 5월 1일
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);     // 5월 31일

        // 1. 유저별 전월 주문 금액 합계 조회
        List<Object[]> aggregates = orderRepository.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        // 2. Map<Long userId, BigDecimal totalAmount> 형태로 변환
        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));

        // 3. 전체 유저 가져오기
        List<User> users = userRepository.findAll();

        // 4. 등급 변경 대상과 로그 저장용 리스트 생성
        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLog> logs = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();
            BigDecimal totalAmount = totalAmountByUser.getOrDefault(userId, BigDecimal.ZERO);

            MembershipLevel oldLevel = user.getMembershipLevel();
            MembershipLevel newLevel = calculateLevel(totalAmount);

            // 등급 정보는 항상 갱신 (변화 없어도 타임스탬프 포함)
            user.setMembershipLevel(newLevel);
            user.setLastMembershipChange(LocalDateTime.now());
            updatedUsers.add(user);

            // 등급 변화 방향에 따라 메시지 작성
            String reason;
            if (newLevel.ordinal() > oldLevel.ordinal()) {
                reason = "자동 강등 (전월 주문합계: " + totalAmount + ")";
            } else if (newLevel.ordinal() < oldLevel.ordinal()) {
                reason = "자동 승급 (전월 주문합계: " + totalAmount + ")";
            } else {
                reason = "등급 유지 (전월 주문합계: " + totalAmount + ")";
            }

            // 로그 객체 생성
            MembershipLog log = new MembershipLog();
            log.setUser(user);
            log.setPreviousLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setChangeReason(reason);
            log.setChangedAt(LocalDateTime.now());

            logs.add(log);
        }

        // 5. saveAll로 한 번에 저장
        userRepository.saveAll(updatedUsers);
        membershipLogRepository.saveAll(logs);

        watch.stop(); // 종료 시간 기록
        log.info("✅ 등급 갱신 배치 완료 - 처리 유저 수: {}, 총 소요 시간: {} ms",
                updatedUsers.size(),
                watch.getTotalTimeMillis());
    }
    public void renewMembershipLevelJpaUpdateInsertForeach(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(1);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        List<Object[]> aggregates = orderRepository.sumOrderAmountByUserBetween(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );

        Map<Long, BigDecimal> totalAmountByUser = aggregates.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));

        List<User> users = userRepository.findAll();

        List<User> updatedUsers = new ArrayList<>();
        List<MembershipLogRequest> logs = new ArrayList<>();

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

            MembershipLogRequest logReq = new MembershipLogRequest();
            logReq.setUserId(userId);
            logReq.setPreviousLevel(oldLevel);
            logReq.setNewLevel(newLevel);
            logReq.setChangeReason(reason);
            logReq.setChangedAt(now);
            logs.add(logReq);
        }

        userRepository.saveAll(updatedUsers);
        membershipLogMapper.bulkInsertRequests(logs);
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


}
