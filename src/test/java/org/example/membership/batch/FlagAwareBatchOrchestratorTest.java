package org.example.membership.batch;

import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.WasInstance;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.StepExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
class FlagAwareBatchOrchestratorTest {

    @MockBean(name = "wasInstanceRegistrar")
    private ApplicationRunner mockWasInstanceRegistrar;

    @Autowired
    private FlagAwareBatchOrchestrator orchestrator;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @Autowired
    private WasInstanceRepository wasInstanceRepository; // ✅ 진짜 DB에 저장할 거니까 Autowired

    @MockBean
    private JpaOrderService jpaOrderService;

    @MockBean
    private JpaBadgeService jpaBadgeService;

    @MockBean
    private JpaMembershipService jpaMembershipService;

    @MockBean
    private FlagManager flagManager;

    @MockBean
    private BadgeBatchExecutor badgeBatchExecutor;

    @MockBean
    private UserLevelBatchExecutor userLevelBatchExecutor;

    @MockBean
    private CouponBatchExecutor couponBatchExecutor;

    @MockBean
    private StepExecutionLogRepository stepExecutionLogRepository;

    @MockBean
    private org.example.membership.config.MyWasInstanceHolder myWasInstanceHolder;

    private UUID uuid;

    @BeforeEach
    void setupMocks() {
        uuid = UUID.randomUUID();

        //  내 wasInstanceHolder가 이 UUID라고 설정
        when(myWasInstanceHolder.getMyUuid()).thenReturn(uuid);
        when(myWasInstanceHolder.getMyIndex()).thenReturn(0);
        when(myWasInstanceHolder.getTotalWases()).thenReturn(1);

        // 실제 DB에 wasInstance insert
        WasInstance wasInstance = new WasInstance();
        wasInstance.setId(uuid);
        wasInstanceRepository.save(wasInstance);
        wasInstanceRepository.flush();

        //  나머지 mock
        when(stepExecutionLogRepository.findByTargetDateAndWasIndex(any(), anyInt()))
                .thenReturn(Optional.empty());
        when(stepExecutionLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(jpaOrderService.aggregateUserCategoryStats(any())).thenReturn(Collections.emptyMap());
        when(jpaMembershipService.getAllUsers()).thenReturn(Collections.emptyList());
        when(jpaBadgeService.detectBadgeUpdateTargets(anyList(), anyMap())).thenReturn(Collections.singletonList("1:1"));

        when(flagManager.isBadgeBatchRunning()).thenReturn(false);
        doAnswer(inv -> null).when(flagManager).startGlobalBadgeBatch();
        doAnswer(inv -> null).when(flagManager).resetScaleOutInterruptFlag();

        // 예외 던지는 시점은 여기
        doAnswer(invocation -> {
            if (flagManager.isScaleOutInterrupted()) {
                throw new ScaleOutInterruptedException("scale out");
            }
            return null;
        }).when(badgeBatchExecutor).execute(anyList(), anyInt(), any(BatchExecutionLog.class));

        when(flagManager.isScaleOutInterrupted()).thenReturn(true);
    }

    @Test
    void runFullBatch_whenScaleOutFlagTrue_thenInterruptedStatus() {
        //  예외 발생하는지 검증
        assertThrows(ScaleOutInterruptedException.class,
                () -> orchestrator.runFullBatch("2024-01-01", 10));

        //  상태가 INTERRUPTED로 DB에 저장됐는지 검증
        assertThat(batchExecutionLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(BatchExecutionLog::getStatus)
                .isEqualTo(BatchExecutionLog.BatchStatus.INTERRUPTED);
    }
}
