package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.domain.log.jdbc.JdbcMembershipLogRepository;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.jpa.UserRepository;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.MembershipLogRequest;
import org.example.membership.dto.UserResponse;
import org.example.membership.service.jdbc.JdbcMembershipRenewalService;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final JdbcMembershipLogRepository jdbcMembershipLogRepository;
    private final UserRepository userRepository;
    private final JdbcMembershipRenewalService jdbcMembershipRenewalService;

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

    @Operation(summary = "mybatis_batch로 등급 갱신", description = "mybatis_batch로 등급 갱신합니다.")
    @PostMapping("/renew/mybatis/batch/fixed")
    public ResponseEntity<Void> renewFixedDateMyBatisByBatch() {
        StopWatch watch = new StopWatch();
        watch.start();
        myBatisMembershipRenewalService.renewMembershipLevelBatch(LocalDate.of(2025, 6, 1));
        watch.stop();
        log.info("💡 mybatis_batch로 등급 갱신 controller 시간: {} ms", watch.getTotalTimeMillis());
        return ResponseEntity.ok().build();

    }


    @Operation(
            summary = "등급 자동 갱신 + 로그 저장 (JDBC 배치)",
            description = "결제 집계 데이터를 기반으로 회원 등급을 계산하고, 사용자 등급을 업데이트하며 로그를 배치 저장합니다."
    )
    @PostMapping("/jdbc/renew")
    public ResponseEntity<Map<String, Object>> renewAndInsertMembershipLogs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate
    ) {
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        // 핵심: 여기서 계산 + 업데이트 + insert 다 수행
        jdbcMembershipRenewalService.renewMembershipLevel(LocalDate.of(2025, 6, 1));

        return ResponseEntity.ok(Map.of(
                "message", "등급 갱신 및 로그 저장 완료",
                "targetDate", targetDate
        ));
    }


} 