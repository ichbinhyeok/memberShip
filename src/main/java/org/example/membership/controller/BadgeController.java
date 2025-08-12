package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.dto.BadgeActivationRequest;
import org.example.membership.dto.ManualBadgeUpdateRequest;
import org.example.membership.service.jpa.JpaBadgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.example.membership.config.MyWasInstanceHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badges") // API 버전 명시
public class BadgeController {

    private final JpaBadgeService jpaBadgeService;
    // private final FlagManager flagManager; // 배치 플래그 의존성 제거
    private final MyWasInstanceHolder myWasInstanceHolder;

    /*
     * LEGACY: 이 엔드포인트는 스냅샷 배치 아키텍처 도입으로 인해 더 이상 사용되지 않습니다.
     * 모든 배치성 업데이트는 SnapshotBatchOrchestrator를 통해 실행됩니다.
     *
    @PostMapping("/update")
    public ResponseEntity<List<Badge>> updateBadgeStates(@RequestBody BadgeUpdateRequest request) {
        // ... 기존 로직 ...
        // 에러 발생 지점: jpaBadgeService.updateBadgeStatesForUser(request.getUser(), request.getStatMap());
        return null;
    }
    */

    /**
     * [API용] 관리자가 배지 활성 상태를 수동으로 변경합니다.
     */
    @PostMapping("/activate")
    public ResponseEntity<String> activate(@RequestBody BadgeActivationRequest request) {
        // isMyUser와 같은 샤딩 로직은 API 게이트웨이나 서비스 메시 레벨에서 처리하는 것이 일반적이지만,
        // 여기서는 애플리케이션 레벨에 유지합니다.
        if (!myWasInstanceHolder.isMyUser(request.getUserId())) {
            // 이 요청을 처리할 올바른 WAS로 리다이렉트하거나, 클라이언트가 직접 올바른 노드로 요청하도록 유도할 수 있습니다.
            // 여기서는 간단히 요청을 거부합니다.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("This request should be handled by another WAS instance.");
        }

        // 새로운 아키텍처에서는 배치 실행과 API가 분리되어 FlagManager를 통한 확인이 불필요해졌습니다.
        jpaBadgeService.changeBadgeActivation(
                request.getUserId(),
                request.getCategoryId(),
                request.isActive()
        );
        return ResponseEntity.ok("OK");
    }

    /**
     * [API용] 단일 배지 상태를 '오늘 기준 통계'로 즉시 업데이트합니다.
     */
    @PatchMapping("/manual-update")
    public ResponseEntity<String> manualUpdate(@RequestBody ManualBadgeUpdateRequest request) {
        jpaBadgeService.updateBadge(request.getUserId(), request.getCategoryId());
        return ResponseEntity.ok("OK");
    }
}
