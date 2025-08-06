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

    @PostMapping("/notify-scaleout")
    public ResponseEntity<ScaleOutAckResponse> notifyScaleOut() {
        try {
            flagManager.raiseScaleOutInterruptFlag();  // << 변경된 부분
            log.info("[알림 수신] scaleOutInterruptFlag 세움");
            return ResponseEntity.ok(new ScaleOutAckResponse(true, "플래그 세움 성공"));
        } catch (Exception e) {
            log.error("[알림 수신 실패]", e);
            return ResponseEntity.internalServerError()
                    .body(new ScaleOutAckResponse(false, "플래그 세움 실패: " + e.getMessage()));
        }
    }

}
