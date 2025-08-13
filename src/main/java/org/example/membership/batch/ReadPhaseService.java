package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.User;
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

    private final org.example.membership.repository.jpa.UserRepository userRepository;
    private final JpaOrderService jpaOrderService;

    /**
     * 전체 사용자와 전체 기간 집계만 수행하여 CalcContext 생성
     */
    @Transactional(readOnly = true)
    public CalcContext buildContext(LocalDate targetDate, LocalDateTime cutoffAt, int batchSize) {
        // 전량 사용자 조회
        List<User> allUsers = userRepository.findAll();

        // 전량 집계: targetDate ~ cutoffAt 구간
        Map<String, Boolean> keysToUpdate =
                jpaOrderService.aggregateUserCategoryStats(targetDate, cutoffAt);

        boolean empty = allUsers.isEmpty();
        return new CalcContext(allUsers, keysToUpdate, batchSize, empty);
    }
}
