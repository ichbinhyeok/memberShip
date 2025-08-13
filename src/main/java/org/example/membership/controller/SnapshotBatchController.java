package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.batch.SnapshotBatchOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/batch/snapshot")
public class SnapshotBatchController {

    private final SnapshotBatchOrchestrator snapshotOrchestrator;

    /**
     * 스냅샷 기반의 전체 멤버십 배치를 실행합니다.
     */
    @PostMapping("/run-full")
    public void runFullBatch(@RequestBody Map<String, String> requestBody) {
    //하드 코딩
        LocalDate targetDate = LocalDate.parse(
                requestBody.getOrDefault("targetDate", "2025-08-01")
        );
        int batchSize = Integer.parseInt(requestBody.getOrDefault("batchSize", "1000"));


        snapshotOrchestrator.runFullBatch(targetDate, batchSize);
    }
}
