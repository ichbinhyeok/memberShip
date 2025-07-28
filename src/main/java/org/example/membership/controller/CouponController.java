package org.example.membership.controller;


import lombok.RequiredArgsConstructor;
import org.example.membership.dto.CouponIssueLogDto;
import org.example.membership.dto.ManualCouponIssueRequest;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaCouponService;
import org.example.membership.config.MyWasInstanceHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
public class CouponController {

    private final JpaCouponService jpaCouponService;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @PostMapping("/issue")
    public ResponseEntity<?> issue(@RequestBody ManualCouponIssueRequest request) {
        if (!myWasInstanceHolder.isMyUser(request.getUserId())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("이 요청은 현재 WAS 인스턴스에서 처리하지 않습니다.");
        }

        CouponIssueLog log = jpaCouponService.manualIssueCoupon(request.getUserId(), request.getCouponCode());

        CouponIssueLogDto dto = new CouponIssueLogDto();
        dto.setId(log.getId());
        dto.setUserId(log.getUser().getId());
        dto.setCouponId(log.getCoupon().getId());
        dto.setMembershipLevel(log.getMembershipLevel());
        dto.setIssuedAt(log.getIssuedAt());

        return ResponseEntity.ok(dto);

    }


    @GetMapping("/user/{userId}")
    public List<CouponIssueLog> getUserCoupons(@PathVariable Long userId) {
        return jpaCouponService.getIssuedCouponsByUser(userId);
    }

}