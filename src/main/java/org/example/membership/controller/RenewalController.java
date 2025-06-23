package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.service.pipeline.RenewalPipelineService;
import org.example.membership.service.pipeline.MyBatisRenewalPipelineService;
import org.example.membership.service.jpa.JpaMembershipRenewalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/renewal")
@RequiredArgsConstructor
public class RenewalController {

    private final RenewalPipelineService renewalPipelineService;
    private final MyBatisRenewalPipelineService myBatisRenewalPipelineService;
    private final JpaMembershipRenewalService jpaMembershipRenewalService;

    @PutMapping("/jpa/badge-only")
    public ResponseEntity<Void> badgeOnly() {
        renewalPipelineService.runBadgeOnly(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mybatis/badge-only")
    public ResponseEntity<Void> badgeOnlyMybatis() {
        myBatisRenewalPipelineService.runBadgeOnly(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/jpa/level-only")
    public ResponseEntity<Void> levelOnly() {
        renewalPipelineService.runLevelOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mybatis/level-only")
    public ResponseEntity<Void> levelOnlyMybatis() {
        myBatisRenewalPipelineService.runLevelOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/jpa/log-only")
    public ResponseEntity<Void> logOnly() {
        renewalPipelineService.runLogOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mybatis/log-only")
    public ResponseEntity<Void> logOnlyMybatis() {
        myBatisRenewalPipelineService.runLogOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/jpa/coupon-only")
    public ResponseEntity<Void> couponOnly() {
        renewalPipelineService.runCouponOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mybatis/coupon-only")
    public ResponseEntity<Void> couponOnlyMybatis() {
        myBatisRenewalPipelineService.runCouponOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/jpa/coupon-log-only")
    public ResponseEntity<Void> couponLogOnly() {
        renewalPipelineService.runCouponLogOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mybatis/coupon-log-only")
    public ResponseEntity<Void> couponLogOnlyMybatis() {
        myBatisRenewalPipelineService.runCouponLogOnly();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/full/jpa")
    public ResponseEntity<Void> fullJpa() {
        renewalPipelineService.runFullJpa(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/full/mybatis")
    public ResponseEntity<Void> fullMybatis() {
        myBatisRenewalPipelineService.runFull(LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mixed/jpa-update-mybatis-insert")
    public ResponseEntity<Void> mixed() {
        jpaMembershipRenewalService.renewMembershipLevelJpaUpdateInsertForeach(LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
