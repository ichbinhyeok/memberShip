package org.example.membership.controller;


import lombok.RequiredArgsConstructor;
import org.example.membership.dto.CouponIssueLogDto;
import org.example.membership.dto.ManualCouponIssueRequest;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaCouponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final JpaCouponService jpaCouponService;

    @PostMapping("/issue")
    public CouponIssueLogDto  issue(@RequestBody ManualCouponIssueRequest request) {
        CouponIssueLog log = jpaCouponService.manualIssueCoupon(request.getUserId(), request.getCouponCode());

        CouponIssueLogDto dto = new CouponIssueLogDto();
        dto.setId(log.getId());
        dto.setUserId(log.getUser().getId());
        dto.setCouponId(log.getCoupon().getId());
        dto.setMembershipLevel(log.getMembershipLevel());
        dto.setIssuedAt(log.getIssuedAt());

        return dto;

    }


    @GetMapping("/user/{userId}")
    public List<CouponIssueLog> getUserCoupons(@PathVariable Long userId) {
        return jpaCouponService.getIssuedCouponsByUser(userId);
    }

}