package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.membership.service.DataGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "데이터 생성", description = "테스트 데이터 생성 API")
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataGenerationController {

    private final DataGenerationService dataGenerationService;

    @Operation(summary = "사용자 데이터 생성", description = "지정된 수만큼 사용자 데이터를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> generateUsers(
            @Parameter(description = "생성할 사용자 수", required = true)
            @RequestParam(defaultValue = "30000") int count
    ) {
        long startTime = System.currentTimeMillis();
        int createdCount = dataGenerationService.generateUsers(count);
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok(Map.of(
                "message", "사용자 데이터 생성 완료",
                "createdCount", createdCount,
                "executionTime", (endTime - startTime) + "ms"
        ));
    }

    @Operation(summary = "주문 데이터 생성", description = "지정된 수만큼 주문 데이터를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> generateOrders(
            @Parameter(description = "생성할 주문 수", required = true)
            @RequestParam(defaultValue = "300000") int count
    ) {
        long startTime = System.currentTimeMillis();
        int createdCount = dataGenerationService.generateOrders(count);
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok(Map.of(
                "message", "주문 데이터 생성 완료",
                "createdCount", createdCount,
                "executionTime", (endTime - startTime) + "ms"
        ));
    }

    @Operation(summary = "전체 데이터 생성", description = "사용자와 주문 데이터를 함께 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> generateAllData(
            @Parameter(description = "생성할 사용자 수")
            @RequestParam(defaultValue = "30000") int userCount,
            @Parameter(description = "생성할 주문 수")
            @RequestParam(defaultValue = "300000") int orderCount
    ) {
        long startTime = System.currentTimeMillis();

        int createdUsers = dataGenerationService.generateUsers(userCount);
        int createdOrders = dataGenerationService.generateOrders(orderCount);

        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok(Map.of(
                "message", "전체 데이터 생성 완료",
                "createdUsers", createdUsers,
                "createdOrders", createdOrders,
                "executionTime", (endTime - startTime) + "ms"
        ));
    }



    @Operation(summary = "모든 데이터 삭제", description = "생성된 모든 테스트 데이터를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공")
    })
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllData() {
        dataGenerationService.deleteAllData();
        return ResponseEntity.ok(Map.of("message", "모든 데이터가 삭제되었습니다."));
    }
}