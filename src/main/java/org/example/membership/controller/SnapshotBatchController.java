package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.batch.SnapshotBatchOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        // 요청 본문에서 targetDate와 batchSize를 받을 수 있습니다.
        // 예시를 위해 하드코딩된 값을 사용합니다.
        String targetDate = requestBody.getOrDefault("targetDate", "2025-08-01");
        int batchSize = Integer.parseInt(requestBody.getOrDefault("batchSize", "1000"));

        snapshotOrchestrator.runFullBatch(targetDate, batchSize);
    }
}
