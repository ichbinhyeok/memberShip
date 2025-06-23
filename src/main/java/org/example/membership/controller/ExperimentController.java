package org.example.membership.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Experiment", description = "실험용 등급 갱신 API")
@RestController
@RequestMapping("/experiment/renew")
@RequiredArgsConstructor
@Slf4j
public class ExperimentController {

    private final JpaMembershipRenewalService jpaMembershipRenewalService;
    private final MyBatisMembershipRenewalService myBatisMembershipRenewalService;

    @PostMapping("/mybatis/foreach")
    public ResponseEntity<Void> renewMyBatisForeach() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-foreach 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mybatis/executor-batch")
    public ResponseEntity<Void> renewMyBatisExecutorBatch() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelExecutorBatch(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-executor-batch 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mybatis/combined-batch")
    public ResponseEntity<Void> renewMyBatisCombinedBatch() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelExecutorBatchWithBulkInsert(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-combined-batch 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mybatis/casewhen-bulk")
    public ResponseEntity<Void> renewMyBatisCaseWhenBulk() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelCaseWhenInsertForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("🚀 casewhen-bulk 등급 갱신 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/jpa-update-foreach")
    public ResponseEntity<Void> renewJpaUpdateForeach() {
        StopWatch watch = new StopWatch();
        watch.start();
        jpaMembershipRenewalService.renewMembershipLevelJpaUpdateInsertForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("⛳ jpa-update-foreach 등급 갱신 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }
}
