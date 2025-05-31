package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.domain.user.User;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MembershipController {
    private final JpaMembershipService jpaMembershipService;
    private final MyBatisMembershipService myBatisMembershipService;

    @GetMapping("/{id}/membership/jpa")
    public ResponseEntity<User> getUserMembershipJpa(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(jpaMembershipService.getUserById(userId));
    }

    @GetMapping("/{id}/membership/mybatis")
    public ResponseEntity<MembershipInfoResponse> getUserMembershipMyBatis(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(myBatisMembershipService.getUserById(userId));
    }
} 