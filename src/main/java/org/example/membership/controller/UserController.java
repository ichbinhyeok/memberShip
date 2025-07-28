package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.*;
import org.example.membership.entity.MembershipLog;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.config.MyWasInstanceHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final JpaMembershipService jpaMembershipService;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request) {
        return jpaMembershipService.createUser(request);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return jpaMembershipService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return jpaMembershipService.getUserById(id);
    }

    @GetMapping("/name/{name}")
    public MembershipInfoResponse getUserByName(@PathVariable String name) {
        return jpaMembershipService.getUserByName(name);
    }

    @PatchMapping("/{id}/level")
    public User updateLevel(@PathVariable Long id, @RequestParam MembershipLevel level) {
        return jpaMembershipService.updateMembershipLevel(id, level);
    }

    @GetMapping("/{id}/status")
    public UserStatusResponse getUserStatus(@PathVariable Long id) {
        return jpaMembershipService.getUserStatus(id);
    }

    @GetMapping("/level/{level}")
    public List<User> getUsersByLevel(@PathVariable MembershipLevel level) {
        return jpaMembershipService.getUsersByMembershipLevel(level);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        jpaMembershipService.deleteUser(userId);
    }


    @PostMapping("/level/manual")
    public ResponseEntity<?> manualChange(@RequestBody ManualMembershipLevelChangeRequest request) {
        if (!myWasInstanceHolder.isMyUser(request.getUserId())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("이 요청은 현재 WAS 인스턴스에서 처리하지 않습니다.");
        }
        MembershipLog log = jpaMembershipService.manualChangeLevel(request.getUserId(), request.getNewLevel());
        if (log == null) {
            return ResponseEntity.ok().build();
        }
        MembershipLogResponse resp = new MembershipLogResponse();
        resp.setUserId(log.getUser().getId());
        resp.setPreviousLevel(log.getPreviousLevel());
        resp.setNewLevel(log.getNewLevel());
        resp.setChangeReason(log.getChangeReason());
        resp.setChangedAt(log.getChangedAt());
        return ResponseEntity.ok(resp);
    }
}
