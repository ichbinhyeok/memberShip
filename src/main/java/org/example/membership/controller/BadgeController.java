package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.dto.BadgeActivationRequest;
import org.example.membership.dto.BadgeUpdateRequest;
import org.example.membership.dto.ManualBadgeUpdateRequest;
import org.example.membership.entity.Badge;
import org.example.membership.service.jpa.JpaBadgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.example.membership.config.MyWasInstanceHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/badges")
public class BadgeController {

    private final JpaBadgeService jpaBadgeService;
    private final FlagManager flagManager;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @PostMapping("/update")
    public ResponseEntity<List<Badge>> updateBadgeStates(@RequestBody BadgeUpdateRequest request) {
        if (flagManager.isBadgeBatchRunning()) {
            return ResponseEntity.accepted().build();
        }

        Long userId = request.getUser().getId();
        if (request.getStatMap() != null) {
            for (Long categoryId : request.getStatMap().keySet()) {
                if (flagManager.isBadgeFlagged(userId, categoryId)) {
                    return ResponseEntity.accepted().build();
                }
            }
        }

        List<Badge> result = jpaBadgeService.updateBadgeStatesForUser(request.getUser(), request.getStatMap());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/activate")
    public ResponseEntity<String> activate(@RequestBody BadgeActivationRequest request) {
        if (!myWasInstanceHolder.isMyUser(request.getUserId())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("이 요청은 현재 WAS 인스턴스에서 처리하지 않습니다.");
        }
        if (flagManager.isBadgeBatchRunning()) {
            return ResponseEntity.accepted().build();
        }
        if (flagManager.isBadgeFlagged(request.getUserId(), request.getCategoryId())) {
            return ResponseEntity.accepted().build();
        }

        flagManager.addBadgeFlag(request.getUserId(), request.getCategoryId());
        try {
            jpaBadgeService.changeBadgeActivation(
                    request.getUserId(),
                    request.getCategoryId(),
                    request.isActive()
            );
            return ResponseEntity.ok("OK");
        } finally {
            flagManager.removeBadgeFlag(request.getUserId(), request.getCategoryId());
        }
    }

    @PatchMapping("/manual-update")
    public ResponseEntity<String> manualUpdate(@RequestBody ManualBadgeUpdateRequest request) {
        if (flagManager.isBadgeBatchRunning()) {
            return ResponseEntity.accepted().build();
        }
        if (flagManager.isBadgeFlagged(request.getUserId(), request.getCategoryId())) {
            return ResponseEntity.accepted().build();
        }

        flagManager.addBadgeFlag(request.getUserId(), request.getCategoryId());
        try {
            jpaBadgeService.updateBadge(request.getUserId(), request.getCategoryId());
            return ResponseEntity.ok("OK");
        } finally {
            flagManager.removeBadgeFlag(request.getUserId(), request.getCategoryId());
        }
    }
}