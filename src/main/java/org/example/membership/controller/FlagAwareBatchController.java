package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.batch.FlagAwareBatchOrchestrator;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch/jpa/flag-aware")
public class FlagAwareBatchController {

    private final FlagAwareBatchOrchestrator orchestrator;

    @PostMapping("/full")
    public void runFullBatch(@RequestBody Map<String, Object> requestBody) {
        String targetDate = (String) requestBody.get("targetDate");
        int batchSize = (int) requestBody.get("batchSize");

        // "2025-06-01"과 "500"으로 하드코딩
        targetDate = "2025-06-01";
        batchSize = 500;

        orchestrator.runFullBatch(targetDate, batchSize);
    }

}