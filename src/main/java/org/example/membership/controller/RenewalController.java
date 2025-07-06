package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisRenewalPipelineService;
import org.example.membership.service.jpa.RenewalPipelineService;
import org.example.membership.service.jpa.BadgeService;
import org.example.membership.service.jpa.MembershipService;
import org.example.membership.service.jpa.MembershipLogService;
import org.example.membership.service.jpa.CouponService;
import org.example.membership.service.mybatis.MyBatisBadgeService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.example.membership.service.mybatis.MyBatisCouponLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/renewal")
@RequiredArgsConstructor
@Tag(name = "등급 갱신 파이프라인", description = "1️⃣ 배지 갱신 → 2️⃣ 등급 갱신 → 3️⃣ 로그 기록 → 4️⃣ 쿠폰 발급 → 5️⃣ 쿠폰 로그 기록")
public class RenewalController {

    private final RenewalPipelineService renewalPipelineService;
    private final MyBatisRenewalPipelineService myBatisRenewalPipelineService;
    private final JpaMembershipRenewalService jpaMembershipRenewalService;
    private final BadgeService badgeService;
    private final MembershipService membershipService;
    private final MembershipLogService membershipLogService;
    private final CouponService couponService;
    private final MyBatisBadgeService myBatisBadgeService;
    private final MyBatisMembershipService myBatisMembershipService;
    private final MyBatisCouponLogService myBatisCouponLogService;

    // --- JPA 기반 단계별 실행 ---

    @Operation(summary = "1️⃣ JPA - 배지 갱신")
    @PutMapping("/jpa/1-badge")
    public ResponseEntity<Void> badgeOnly() {
        long start = System.currentTimeMillis();
        badgeService.bulkUpdateBadgeStates(LocalDate.now());
        long end = System.currentTimeMillis();
        System.out.println("⏱ JPA 배지 갱신 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "2️⃣ JPA - 등급 갱신")
    @PutMapping("/jpa/2-level")
    public ResponseEntity<Void> levelOnly() {
        long start = System.currentTimeMillis();
        membershipService.bulkUpdateMembershipLevels();
        long end = System.currentTimeMillis();
        System.out.println("⏱ JPA 등급 갱신 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "3️⃣ JPA - 등급 변경 로그 기록")
    @PutMapping("/jpa/3-log")
    public ResponseEntity<Void> logOnly() {
        long start = System.currentTimeMillis();
        membershipLogService.bulkInsertMembershipLogs();
        long end = System.currentTimeMillis();
        System.out.println("⏱ JPA 로그 기록 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "4️⃣ JPA - 쿠폰 발급")
    @PutMapping("/jpa/4-coupon")
    public ResponseEntity<Void> couponOnly() {
        long start = System.currentTimeMillis();
        couponService.bulkIssueCouponsForAllUsers(1000);
        long end = System.currentTimeMillis();
        System.out.println("⏱ JPA 쿠폰 발급 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "⏱ JPA - 등급 갱신 + 로그 기록 (내부 분리 시간 포함)")
    @PutMapping("/jpa/2-level-log-timing")
    public ResponseEntity<Void> levelAndLogWithInternalTimingJpa() {
        long start = System.currentTimeMillis();
        renewalPipelineService.runLevelAndLog();  // ← 새로 추가한 JPA 서비스 메서드
        long end = System.currentTimeMillis();
        System.out.println("⏱ [컨트롤러] JPA 등급+로그 전체 수행 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }



    // --- MyBatis 기반 단계별 실행 ---

    @Operation(summary = "1️⃣ MyBatis - 배지 갱신")
    @PutMapping("/mybatis/1-badge")
    public ResponseEntity<Void> badgeOnlyMybatis() {
        long start = System.currentTimeMillis();
        myBatisBadgeService.bulkUpdateBadgeStates(LocalDate.now());
        long end = System.currentTimeMillis();
        System.out.println("⏱ MyBatis 배지 갱신 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "2️⃣ MyBatis - 등급 갱신")
    @PutMapping("/mybatis/2-level")
    public ResponseEntity<Void> levelOnlyMybatis() {
        long start = System.currentTimeMillis();
        myBatisMembershipService.bulkUpdateMembershipLevels();
        long end = System.currentTimeMillis();
        System.out.println("⏱ MyBatis 등급 갱신 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "3️⃣ MyBatis - 등급 변경 로그 기록")
    @PutMapping("/mybatis/3-log")
    public ResponseEntity<Void> logOnlyMybatis() {
        long start = System.currentTimeMillis();
        myBatisMembershipService.bulkInsertMembershipLogs();
        long end = System.currentTimeMillis();
        System.out.println("⏱ MyBatis 로그 기록 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "4️⃣ MyBatis - 쿠폰 발급")
    @PutMapping("/mybatis/4-coupon")
    public ResponseEntity<Void> couponOnlyMybatis() {
        long start = System.currentTimeMillis();
        myBatisCouponLogService.bulkIssueCouponsForAllUsers();
        long end = System.currentTimeMillis();
        System.out.println("⏱ MyBatis 쿠폰 발급 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }



    @Operation(summary = "⏱ MyBatis - 등급 갱신 + 로그 기록 (내부 분리 시간 포함)")
    @PutMapping("/mybatis/2-level-log-timing")
    public ResponseEntity<Void> levelAndLogWithInternalTiming() {
        long start = System.currentTimeMillis();
        myBatisRenewalPipelineService.runLevelAndLog();
        long end = System.currentTimeMillis();

        System.out.println("⏱ [컨트롤러] 전체 수행 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }



    // --- 통합 실행 ---

    @Operation(summary = "✅ JPA - 전체 파이프라인 실행")
    @PutMapping("/full/jpa")
    public ResponseEntity<Void> fullJpa() {
        long start = System.currentTimeMillis();
        renewalPipelineService.runFullJpa(LocalDate.now());
        long end = System.currentTimeMillis();
        System.out.println("⏱ JPA 전체 파이프라인 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "✅ MyBatis - 전체 파이프라인 실행")
    @PutMapping("/full/mybatis")
    public ResponseEntity<Void> fullMybatis() {
        long start = System.currentTimeMillis();
        myBatisRenewalPipelineService.runFull(LocalDate.now());
        long end = System.currentTimeMillis();
        System.out.println("⏱ MyBatis 전체 파이프라인 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "✅ 혼합 전략 - JPA update + MyBatis insert")
    @PutMapping("/mixed/jpa-update-mybatis-insert")
    public ResponseEntity<Void> mixed() {
        long start = System.currentTimeMillis();
        jpaMembershipRenewalService.renewMembershipLevelJpaUpdateInsertForeach(LocalDate.now());
        long end = System.currentTimeMillis();
        System.out.println("⏱ 혼합 전략 파이프라인 소요 시간: " + (end - start) + "ms");
        return ResponseEntity.ok().build();
    }
}
