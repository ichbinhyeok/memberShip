package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.BadgeActivationRequest;
import org.example.membership.dto.BadgeUpdateRequest;
import org.example.membership.dto.ManualBadgeActivationRequest;
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
    public List<Badge> updateBadgeStates(@RequestBody BadgeUpdateRequest request ){
        return jpaBadgeService.updateBadgeStatesForUser(request.getUser(), request.getStatMap());
    }

    @PostMapping("/activate")
        public Badge activate(@RequestBody BadgeActivationRequest request) {
            return jpaBadgeService.changeBadgeActivation(
                    request.getUserId(),
                    request.getCategoryId(),
                    request.isActive()
            );
        }
}
