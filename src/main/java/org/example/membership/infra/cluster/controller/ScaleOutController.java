// org/example/membership/infra/cluster/controller/ScaleOutController.java
package org.example.membership.infra.cluster.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.infra.cluster.dto.ScaleOutAckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ScaleOutController {

    private final FlagManager flagManager;



    // 배지 플래그 해제: 스케줄러 리더 WAS가 배치 종료 후 호출
    @PostMapping("/internal/batch/badge-flag/off")
    public ResponseEntity<ScaleOutAckResponse> badgeFlagOff() {
        try {
            flagManager.removeBadgeFlag(-1L, -1L);
            log.info("[배지 배치] 플래그 해제 수신(-1:-1)");
            return ResponseEntity.ok(new ScaleOutAckResponse(true, "배지 플래그 해제 완료"));
        } catch (Exception e) {
            log.error("[배지 플래그 해제 실패]", e);
            return ResponseEntity.internalServerError()
                    .body(new ScaleOutAckResponse(false, "배지 플래그 해제 실패: " + e.getMessage()));
        }
    }

    //  배지 플래그 ON
    @PostMapping("/internal/batch/badge-flag/on")
    public ResponseEntity<ScaleOutAckResponse> badgeFlagOn() {
        try {
            flagManager.addBadgeFlag(-1L, -1L);
            log.info("[배지 배치] 플래그 설정 수신(-1:-1)");
            return ResponseEntity.ok(new ScaleOutAckResponse(true, "배지 플래그 설정 완료"));
        } catch (Exception e) {
            log.error("[배지 플래그 설정 실패]", e);
            return ResponseEntity.internalServerError()
                    .body(new ScaleOutAckResponse(false, "배지 플래그 설정 실패: " + e.getMessage()));
        }
    }

/*레거시*/
    //    // 기존 스케일아웃 알림(그대로 유지)
//    @PostMapping("/notify-scaleout")
//    public ResponseEntity<ScaleOutAckResponse> notifyScaleOut() {
//        try {
//            flagManager.raiseScaleOutInterruptFlag();
//            log.info("[알림 수신] scaleOutInterruptFlag 세움");
//            return ResponseEntity.ok(new ScaleOutAckResponse(true, "플래그 세움 성공"));
//        } catch (Exception e) {
//            log.error("[알림 수신 실패]", e);
//            return ResponseEntity.internalServerError()
//                    .body(new ScaleOutAckResponse(false, "플래그 세움 실패: " + e.getMessage()));
//        }
//    }
}
