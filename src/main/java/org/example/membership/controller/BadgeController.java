package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaBadgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/badges")
public class BadgeController {

    private final JpaBadgeService jpaBadgeService;

    @PostMapping("/update")
    public List<Badge> updateBadgeStates(@RequestBody User user,
                                         @RequestBody(required = false) Map<Long, OrderCountAndAmount> statMap) {
        return jpaBadgeService.updateBadgeStatesForUser(user, statMap);
    }
}
