package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

@Tag(name = "예시 API", description = "Swagger 예시 API")
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    @Operation(summary = "예시 API", description = "Swagger 문서화 예시를 위한 API입니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public String example(
        @Parameter(description = "예시 ID", required = true) @PathVariable Long id
    ) {
        return "Example response for id: " + id;
    }
} 