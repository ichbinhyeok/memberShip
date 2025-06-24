package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisRenewalPipelineService;
import org.example.membership.service.jpa.RenewalPipelineService;
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

    // --- JPA 기반 단계별 실행 ---

    @Operation(summary = "1️⃣ JPA - 배지 갱신")
    @PutMapping("/jpa/1-badge")
    public ResponseEntity<Void> badgeOnly() {
        renewalPipelineService.runBadgeOnly(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "2️⃣ JPA - 등급 갱신")
    @PutMapping("/jpa/2-level")
    public ResponseEntity<Void> levelOnly() {
        renewalPipelineService.runLevelOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "3️⃣ JPA - 등급 변경 로그 기록")
    @PutMapping("/jpa/3-log")
    public ResponseEntity<Void> logOnly() {
        renewalPipelineService.runLogOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "4️⃣ JPA - 쿠폰 발급")
    @PutMapping("/jpa/4-coupon")
    public ResponseEntity<Void> couponOnly() {
        renewalPipelineService.runCouponOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "5️⃣ JPA - 쿠폰 발급 로그 기록")
    @PutMapping("/jpa/5-coupon-log")
    public ResponseEntity<Void> couponLogOnly() {
        renewalPipelineService.runCouponLogOnly();
        return ResponseEntity.ok().build();
    }

    // --- MyBatis 기반 단계별 실행 ---

    @Operation(summary = "1️⃣ MyBatis - 배지 갱신")
    @PutMapping("/mybatis/1-badge")
    public ResponseEntity<Void> badgeOnlyMybatis() {
        myBatisRenewalPipelineService.runBadgeOnly(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "2️⃣ MyBatis - 등급 갱신")
    @PutMapping("/mybatis/2-level")
    public ResponseEntity<Void> levelOnlyMybatis() {
        myBatisRenewalPipelineService.runLevelOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "3️⃣ MyBatis - 등급 변경 로그 기록")
    @PutMapping("/mybatis/3-log")
    public ResponseEntity<Void> logOnlyMybatis() {
        myBatisRenewalPipelineService.runLogOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "4️⃣ MyBatis - 쿠폰 발급")
    @PutMapping("/mybatis/4-coupon")
    public ResponseEntity<Void> couponOnlyMybatis() {
        myBatisRenewalPipelineService.runCouponOnly();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "5️⃣ MyBatis - 쿠폰 발급 로그 기록")
    @PutMapping("/mybatis/5-coupon-log")
    public ResponseEntity<Void> couponLogOnlyMybatis() {
        myBatisRenewalPipelineService.runCouponLogOnly();
        return ResponseEntity.ok().build();
    }

    // --- 통합 실행 ---

    @Operation(summary = "✅ JPA - 전체 파이프라인 실행")
    @PutMapping("/full/jpa")
    public ResponseEntity<Void> fullJpa() {
        renewalPipelineService.runFullJpa(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "✅ MyBatis - 전체 파이프라인 실행")
    @PutMapping("/full/mybatis")
    public ResponseEntity<Void> fullMybatis() {
        myBatisRenewalPipelineService.runFull(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "✅ 혼합 전략 - JPA update + MyBatis insert")
    @PutMapping("/mixed/jpa-update-mybatis-insert")
    public ResponseEntity<Void> mixed() {
        jpaMembershipRenewalService.renewMembershipLevelJpaUpdateInsertForeach(LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
