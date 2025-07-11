package org.example.membership.controller;


import lombok.RequiredArgsConstructor;
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
    public CouponIssueLog issue(@RequestBody ManualCouponIssueRequest request) {
        return jpaCouponService.manualIssueCoupon(request.getUserId(), request.getCouponCode());
    }

    @GetMapping("/user/{userId}")
    public List<CouponIssueLog> getUserCoupons(@PathVariable Long userId) {
        return jpaCouponService.getIssuedCouponsByUser(userId);
    }

}