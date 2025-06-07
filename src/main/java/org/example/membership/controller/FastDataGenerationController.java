package org.example.membership.controller;


import lombok.RequiredArgsConstructor;
import org.example.membership.service.FastDataGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev/data")
@RequiredArgsConstructor
public class FastDataGenerationController {

    private final FastDataGenerationService fastDataGenerationService;

    @PostMapping("/users/{count}")
    public ResponseEntity<String> generateUsers(@PathVariable("count") int count) {
        fastDataGenerationService.generateUsers(count);
        return ResponseEntity.ok(count + "명 사용자 생성 완료");
    }

    @PostMapping("/orders/{count}")
    public ResponseEntity<String> generateOrders(@PathVariable("count") int count) {
        fastDataGenerationService.generateOrders(count);
        return ResponseEntity.ok(count + "건 주문 생성 완료");
    }
}
