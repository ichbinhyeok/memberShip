package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.UserResponse;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "멤버십 관리", description = "멤버십 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class MembershipController {
    private final JpaMembershipService jpaMembershipService;
    private final MyBatisMembershipService myBatisMembershipService;
    private final JpaMembershipRenewalService jpaMembershipRenewalService;
    private final MyBatisMembershipRenewalService myBatisMembershipRenewalService;
    private final UserRepository userRepository;

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

    @Operation(summary = "jpa로 등급 갱신", description = "jpa로 등급 갱신합니다.")
    @PostMapping("/renew/fixed")
    public ResponseEntity<Void> renewFixedDate() {
        StopWatch watch = new StopWatch();
        watch.start();
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 jpa로 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();

    }
    @Operation(summary = "mybatis로 등급 갱신", description = "mybatis로 등급 갱신합니다.")
    @PostMapping("/renew/mybatis/fixed")
    public ResponseEntity<Void> renewFixedDateMyBatis() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis로 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();

    }
    @PostMapping("/renew/mybatis/foreach")
    public ResponseEntity<Void> renewByForeach() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-foreach 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/renew/mybatis/executor-batch")
    public ResponseEntity<Void> renewByExecutorBatch() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelExecutorBatch(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-executor-batch 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }




    @PostMapping("/renew/mybatis/combined-batch")
    public ResponseEntity<Void> renewWithExecutorBatchAndBulkInsert() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelExecutorBatchWithBulkInsert(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis-combined-batch 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/renew/mybatis/casewhen-bulk")
    public ResponseEntity<Void> renewWithCaseWhenBulkUpdate() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelCaseWhenInsertForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("🚀 casewhen-bulk 등급 갱신 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/renew/jpa-update-foreach")
    public ResponseEntity<Void> renewJpaUpdateForeachInsert() {
        StopWatch watch = new StopWatch();
        watch.start();
        jpaMembershipRenewalService.renewMembershipLevelJpaUpdateInsertForeach(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("⛳ jpa-update-foreach 등급 갱신 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<org.example.membership.dto.UserStatusResponse> getStatus(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(jpaMembershipService.getUserStatus(userId));
    }

    @GetMapping("/{id}/coupons")
    public ResponseEntity<java.util.List<org.example.membership.dto.CouponInfoResponse>> getCoupons(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(jpaMembershipService.getUserCoupons(userId));
    }


} 