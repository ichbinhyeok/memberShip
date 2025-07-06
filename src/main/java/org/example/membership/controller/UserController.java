package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.CreateUserRequest;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.UserStatusResponse;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaMembershipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final JpaMembershipService jpaMembershipService;

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

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        jpaMembershipService.deleteUser(id);
    }
}
