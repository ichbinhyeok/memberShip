// ReadPhaseService.java
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadPhaseService {

    private final UserRepository userRepository;
    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;

    @Transactional(readOnly = true)
    public CalcContext buildContext(LocalDate targetDate,
                                    LocalDateTime cutoffAt,
                                    long minId, long maxId,
                                    int index, int total,
                                    int batchSize) {
        long totalSpan = maxId - minId + 1;
        long rangeSize = Math.max(1, (long) Math.ceil((double) totalSpan / total));
        long rangeStart = minId + (long) index * rangeSize;
        long rangeEnd   = (index == total - 1) ? maxId : Math.min(maxId, rangeStart + rangeSize - 1);

        if (rangeStart > maxId) {
            return CalcContext.empty(rangeStart, rangeEnd, index, total);
        }

        // 1) 범위 제한 집계
        Map<Long, Map<Long, OrderCountAndAmount>> statMap =
                jpaOrderService.aggregateUserCategoryStats(targetDate, cutoffAt, rangeStart, rangeEnd);

        // 2) 범위 유저 조회
        List<User> myUsers = userRepository.findUsersInRange(rangeStart, rangeEnd);

        // 3) 배지 대상 선별(읽기 연산)
        Map<String, Boolean> keysToUpdate = jpaBadgeService.detectBadgeUpdateTargets(myUsers, statMap);

        return CalcContext.of(rangeStart, rangeEnd, index, total, myUsers, statMap, keysToUpdate, batchSize);
    }
}
