package org.example.membership.controller;

 import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.membership.service.MixedMembershipService;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Membership Renewal", description = "등급 갱신 운영 API - JPA / MyBatis / 혼합 전략 지원")
@RestController
@RequestMapping("/renewal")
@RequiredArgsConstructor
public class MembershipController {

    private final JpaMembershipRenewalService jpaMembershipRenewalService;
    private final MyBatisMembershipRenewalService myBatisMembershipRenewalService;
    private final MixedMembershipService mixedMembershipService;

    // --- 전체 실행 ---

    @Operation(summary = "JPA 기반 전체 등급 갱신", description = "JPA를 사용하여 통계, 등급 변경, 쿠폰 발급을 모두 수행합니다.")
    @ApiResponse(responseCode = "200", description = "JPA 전체 갱신 완료")
    @PutMapping("/full/jpa")
    public ResponseEntity<Void> renewAllJpa() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "MyBatis 기반 전체 등급 갱신", description = "MyBatis를 사용하여 통계, 등급 변경, 쿠폰 발급을 모두 수행합니다.")
    @ApiResponse(responseCode = "200", description = "MyBatis 전체 갱신 완료")
    @PutMapping("/full/mybatis")
    public ResponseEntity<Void> renewAllMyBatis() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "혼합 전략 전체 등급 갱신", description = "JPA와 MyBatis를 조합하여 통계, 등급 변경, 쿠폰 발급을 모두 수행합니다.")
    @ApiResponse(responseCode = "200", description = "혼합 전략 전체 갱신 완료")
    @PutMapping("/full/mixed")
    public ResponseEntity<Void> renewAllMixed() {
        mixedMembershipService.renewAll(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : JPA ---

    @Operation(summary = "JPA 통계 갱신", description = "주문 데이터를 기반으로 뱃지 통계를 생성합니다.")
    @PutMapping("/step/jpa/statistics")
    public ResponseEntity<Void> stepJpaStatistics() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "JPA 등급 갱신", description = "뱃지를 기준으로 사용자 등급을 계산하고 저장합니다.")
    @PutMapping("/step/jpa/membership")
    public ResponseEntity<Void> stepJpaMembership() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "JPA 쿠폰 발급", description = "갱신된 등급을 기반으로 쿠폰을 발급합니다.")
    @PutMapping("/step/jpa/coupon")
    public ResponseEntity<Void> stepJpaCoupon() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : MyBatis ---

    @Operation(summary = "MyBatis 통계 갱신", description = "주문 데이터를 기반으로 뱃지 통계를 생성합니다.")
    @PutMapping("/step/mybatis/statistics")
    public ResponseEntity<Void> stepMyBatisStatistics() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "MyBatis 등급 갱신", description = "뱃지를 기준으로 사용자 등급을 계산하고 저장합니다.")
    @PutMapping("/step/mybatis/membership")
    public ResponseEntity<Void> stepMyBatisMembership() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "MyBatis 쿠폰 발급", description = "갱신된 등급을 기반으로 쿠폰을 발급합니다.")
    @PutMapping("/step/mybatis/coupon")
    public ResponseEntity<Void> stepMyBatisCoupon() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : 혼합 전략 ---

    @Operation(summary = "혼합 전략 통계 갱신", description = "JPA를 사용하여 주문 통계를 집계하고 뱃지를 부여합니다.")
    @PutMapping("/step/mixed/statistics")
    public ResponseEntity<Void> stepMixedStatistics() {
        mixedMembershipService.runStatistics(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "혼합 전략 등급 갱신", description = "JPA를 사용하여 등급을 갱신합니다.")
    @PutMapping("/step/mixed/membership")
    public ResponseEntity<Void> stepMixedMembership() {
        mixedMembershipService.runMembership(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "혼합 전략 쿠폰 발급", description = "MyBatis를 사용하여 쿠폰을 발급하고 로그를 남깁니다.")
    @PutMapping("/step/mixed/coupon")
    public ResponseEntity<Void> stepMixedCoupon() {
        mixedMembershipService.runCoupon(LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
