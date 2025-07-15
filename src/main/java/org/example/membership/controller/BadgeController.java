package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.UserCategoryProcessingFlagManager;
import org.example.membership.dto.BadgeActivationRequest;
import org.example.membership.dto.BadgeUpdateRequest;
import org.example.membership.dto.ManualBadgeUpdateRequest;
import org.example.membership.entity.Badge;
import org.example.membership.service.jpa.JpaBadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/badges")
public class BadgeController {

    private final JpaBadgeService jpaBadgeService;
    private final UserCategoryProcessingFlagManager flagManager;

    @PostMapping("/update")
    public List<Badge> updateBadgeStates(@RequestBody BadgeUpdateRequest request ){
        return jpaBadgeService.updateBadgeStatesForUser(request.getUser(), request.getStatMap());
    }

    @PostMapping("/activate")
    public ResponseEntity<String> activate(@RequestBody BadgeActivationRequest request) {
        String key = request.getUserId() + "-" + request.getCategoryId();
        if (!flagManager.mark(key)) {
            return ResponseEntity.accepted()
                    .body("현재 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
        try {
            jpaBadgeService.changeBadgeActivation(
                    request.getUserId(),
                    request.getCategoryId(),
                    request.isActive()
            );
            return ResponseEntity.ok("OK");
        } finally {
            flagManager.clear(key);
        }
    }

    @PatchMapping("/manual-update")
    public ResponseEntity<String> manualUpdate(@RequestBody ManualBadgeUpdateRequest request) {
        String key = request.getUserId() + "-" + request.getCategoryId();
        if (!flagManager.mark(key)) {
            return ResponseEntity.accepted()
                    .body("현재 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
        try {
            jpaBadgeService.updateBadge(request.getUserId(), request.getCategoryId());
            return ResponseEntity.ok("OK");
        } finally {
            flagManager.clear(key);
        }
    }
}
