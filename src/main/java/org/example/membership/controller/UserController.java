package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.UserResponse;
import org.example.membership.dto.UserStatusResponse;
import org.example.membership.service.jpa.JpaMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "멤버십 관리", description = "사용자 등록 및 조회 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final JpaMembershipService membershipService;

    @Operation(summary = "사용자 멤버십 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
    @GetMapping("/{id}/membership")
    public ResponseEntity<MembershipInfoResponse> getUserMembership(
            @Parameter(description = "사용자 ID", required = true) @PathVariable("id") Long userId) {
        return ResponseEntity.ok(MembershipInfoResponse.from(membershipService.getUserById(userId)));
    }

    @Operation(summary = "사용자 이름으로 멤버십 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
    @GetMapping("/name/{userName}/membership")
    public ResponseEntity<MembershipInfoResponse> getUserMembershipByName(
            @Parameter(description = "사용자 이름", required = true) @PathVariable("userName") String userName) {
        return ResponseEntity.ok(membershipService.getUserByName(userName));
    }

    @Operation(summary = "사용자 생성")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "생성 성공")})
    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(UserResponse.from(membershipService.createUser(request)));
    }

    @Operation(summary = "사용자 상태 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
    @GetMapping("/{id}/status")
    public ResponseEntity<UserStatusResponse> getStatus(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(membershipService.getUserStatus(userId));
    }

    @Operation(summary = "사용자 쿠폰 목록 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
    @GetMapping("/{id}/coupons")
    public ResponseEntity<List<org.example.membership.dto.CouponInfoResponse>> getCoupons(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(membershipService.getUserCoupons(userId));
    }
}
