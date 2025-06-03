package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.membership.domain.user.User;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.UserResponse;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "멤버십 관리", description = "멤버십 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MembershipController {
    private final JpaMembershipService jpaMembershipService;
    private final MyBatisMembershipService myBatisMembershipService;

    @Operation(summary = "JPA로 사용자 멤버십 조회", description = "사용자 ID로 JPA를 사용하여 멤버십 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{id}/membership/jpa")
    public ResponseEntity<User> getUserMembershipJpa(
        @Parameter(description = "사용자 ID", required = true) @PathVariable("id") Long userId
    ) {
        return ResponseEntity.ok(jpaMembershipService.getUserById(userId));
    }

    @Operation(summary = "MyBatis로 사용자 멤버십 조회", description = "사용자 ID로 MyBatis를 사용하여 멤버십 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{id}/membership/mybatis")
    public ResponseEntity<MembershipInfoResponse> getUserMembershipMyBatis(
        @Parameter(description = "사용자 ID", required = true) @PathVariable("id") Long userId
    ) {
        return ResponseEntity.ok(myBatisMembershipService.getUserById(userId));
    }

    @Operation(summary = "JPA로 사용자 이름으로 멤버십 조회", description = "사용자 이름으로 JPA를 사용하여 멤버십 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/name/{userName}/membership/jpa")
    public ResponseEntity<MembershipInfoResponse> getUserMembershipJpa(
        @Parameter(description = "사용자 이름", required = true) @PathVariable("userName") String userName
    ) {
        return ResponseEntity.ok(jpaMembershipService.getUserByName(userName));
    }

    @Operation(summary = "MyBatis로 사용자 이름으로 멤버십 조회", description = "사용자 이름으로 MyBatis를 사용하여 멤버십 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/name/{userName}/membership/mybatis")
    public ResponseEntity<MembershipInfoResponse> getUserMembershipMyBatis(
        @Parameter(description = "사용자 이름", required = true) @PathVariable("userName") String userName
    ) {
        return ResponseEntity.ok(myBatisMembershipService.getUserByUsername(userName));
    }



    @Operation(summary = "JPA로 사용자 생성", description = "JPA를 사용하여 새로운 사용자를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/createUser/jpa")
    public ResponseEntity<UserResponse> createUserJpa(
            @Parameter(description = "사용자 정보", required = true)
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.ok(UserResponse.from(jpaMembershipService.createUser(request)));
    }

    @Operation(summary = "MyBatis로 사용자 생성", description = "MyBatis를 사용하여 새로운 사용자를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/createUser/mybatis")
    public ResponseEntity<UserResponse> createUserMyBatis(
            @Parameter(description = "사용자 정보", required = true)
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.ok(UserResponse.from(myBatisMembershipService.createUser(request)));
    }

} 