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
    public CalcContext buildContext(LocalDate targetDate, LocalDateTime cutoffAt, int batchSize, LocalDateTime batchStartTime) {
        List<User> allUsers = userRepository.findAll();

        Map<Long, Map<Long, OrderCountAndAmount>> statMap =
                jpaOrderService.aggregateUserCategoryStats(targetDate, cutoffAt);

        Map<String, Boolean> keysToUpdate =
                jpaBadgeService.detectBadgeUpdateTargets(allUsers, statMap);

        boolean empty = keysToUpdate.isEmpty();
        if (empty) {
            return CalcContext.createEmpty(batchStartTime);
        }
        return new CalcContext(allUsers, keysToUpdate, batchSize, false, batchStartTime);
    }
}