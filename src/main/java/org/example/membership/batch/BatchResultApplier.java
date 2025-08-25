//  (applyBadgeResultsPaged / applyLevelResultsPaged 교체)
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.BadgeResult;
import org.example.membership.entity.batch.LevelResult;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class BatchResultApplier {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;
    private final TransactionalChunkProcessor chunkProcessor;

    public void applyBadgeResultsPaged(UUID executionId, LocalDateTime t0, int pageSize) {

        UUID afterId = null;
        while (true) {
            List<BadgeResult> page = badgeResultRepository.findPendingAfterId(executionId, afterId, pageSize);
            if (page.isEmpty()) break;

            List<UUID> ids = page.stream().map(BadgeResult::getId).toList();
            chunkProcessor.applyBadgeChunkBulk(ids);

            afterId = page.get(page.size() - 1).getId();
        }
    }

    public void applyLevelResultsPaged(UUID executionId,LocalDateTime t,  int pageSize) {
        UUID afterId = null;
        while (true) {
            List<LevelResult> page = levelResultRepository.findPendingAfterId(executionId, afterId, pageSize);
            if (page.isEmpty()) break;

            List<UUID> ids = page.stream().map(LevelResult::getId).toList();
            chunkProcessor.applyLevelChunkManaged(ids,t);

            afterId = page.get(page.size() - 1).getId();
        }
    }
}