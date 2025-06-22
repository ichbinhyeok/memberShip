package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.service.MixedMembershipService;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.example.membership.service.mybatis.MyBatisMembershipRenewalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/renewal")
@RequiredArgsConstructor
public class MembershipController {

    private final JpaMembershipRenewalService jpaMembershipRenewalService;
    private final MyBatisMembershipRenewalService myBatisMembershipRenewalService;
    private final MixedMembershipService mixedMembershipService;

    // --- 전체 실행 ---
    @PutMapping("/full/jpa")
    public ResponseEntity<Void> renewAllJpa() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/full/mybatis")
    public ResponseEntity<Void> renewAllMyBatis() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/full/mixed")
    public ResponseEntity<Void> renewAllMixed() {
        mixedMembershipService.renewAll(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : JPA ---
    @PutMapping("/step/jpa/statistics")
    public ResponseEntity<Void> stepJpaStatistics() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/jpa/membership")
    public ResponseEntity<Void> stepJpaMembership() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/jpa/coupon")
    public ResponseEntity<Void> stepJpaCoupon() {
        jpaMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : MyBatis ---
    @PutMapping("/step/mybatis/statistics")
    public ResponseEntity<Void> stepMyBatisStatistics() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/mybatis/membership")
    public ResponseEntity<Void> stepMyBatisMembership() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/mybatis/coupon")
    public ResponseEntity<Void> stepMyBatisCoupon() {
        myBatisMembershipRenewalService.renewMembershipLevel(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    // --- 단계별 실행 : 혼합 ---
    @PutMapping("/step/mixed/statistics")
    public ResponseEntity<Void> stepMixedStatistics() {
        mixedMembershipService.runStatistics(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/mixed/membership")
    public ResponseEntity<Void> stepMixedMembership() {
        mixedMembershipService.runMembership(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/step/mixed/coupon")
    public ResponseEntity<Void> stepMixedCoupon() {
        mixedMembershipService.runCoupon(LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
