package org.example.membership.controller;


import lombok.RequiredArgsConstructor;
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
    public List<CouponIssueLog> issueCoupons(@RequestBody User user) {
        return jpaCouponService.issueCoupons(user);
    }
}