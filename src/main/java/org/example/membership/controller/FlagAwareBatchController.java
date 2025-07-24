package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.batch.FlagAwareBatchOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch/jpa/flag-aware")
public class FlagAwareBatchController {

    private final FlagAwareBatchOrchestrator orchestrator;

    @PostMapping("/full")
    public void runFullBatch(@RequestParam String targetDate,
                             @RequestParam(defaultValue = "100") int batchSize) {
        orchestrator.runFullBatch(targetDate, batchSize);
    }


}