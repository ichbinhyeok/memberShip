package org.example.membership.batch;

import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaCouponService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FullBatchStepOrderTest {

    @Autowired
    private FlagAwareBatchOrchestrator orchestrator;
    @Autowired
    private ChunkExecutionLogRepository chunkExecutionLogRepository;

    @MockBean
    private JpaOrderService jpaOrderService;
    @MockBean
    private JpaBadgeService jpaBadgeService;
    @MockBean
    private JpaMembershipService jpaMembershipService;
    @MockBean
    private JpaCouponService jpaCouponService;

    @Test
    void runFullBatch_recordsStepsInOrder() {
        User user = new User();
        user.setId(1001L);
        user.setName("test");

        when(jpaMembershipService.getAllUsers()).thenReturn(List.of(user));
        when(jpaOrderService.aggregateUserCategoryStats(any())).thenReturn(Collections.emptyMap());
        when(jpaBadgeService.detectBadgeUpdateTargets(anyList(), anyMap())).thenReturn(List.of("1001:1"));
        doNothing().when(jpaBadgeService).bulkUpdateBadgeStates(anyList(), anyInt());
        doNothing().when(jpaMembershipService).bulkUpdateMembershipLevelsAndLog(anyList(), anyMap(), anyInt());
        doNothing().when(jpaCouponService).bulkIssueCoupons(anyList(), anyInt());

        orchestrator.runFullBatch("2024-01-01", 1000);

        List<ChunkExecutionLog> logs = chunkExecutionLogRepository.findAll(Sort.by("recordedAt"));
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getStepType()).isEqualTo(ChunkExecutionLog.StepType.BADGE);
        assertThat(logs.get(1).getStepType()).isEqualTo(ChunkExecutionLog.StepType.LEVEL);
        assertThat(logs.get(2).getStepType()).isEqualTo(ChunkExecutionLog.StepType.COUPON);
    }
}