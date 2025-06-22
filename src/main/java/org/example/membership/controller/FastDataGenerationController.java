package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.service.FastDataGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev/data")
@RequiredArgsConstructor
public class FastDataGenerationController {

    private final FastDataGenerationService fastDataGenerationService;

    @PostMapping("/generate/all")
    public ResponseEntity<String> generateAll() {
        fastDataGenerationService.generateAll();
        return ResponseEntity.ok("모든 데이터 생성 완료");
    }
}
