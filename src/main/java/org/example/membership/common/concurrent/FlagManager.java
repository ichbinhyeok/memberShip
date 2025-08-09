package org.example.membership.common.concurrent;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FlagManager {

    // 전역 API 게이트(클러스터 쓰기/변경 차단)
    private final AtomicBoolean globalApiGate = new AtomicBoolean(false);

    // 단일 WAS 오케스트레이터 재진입 방지(프로세스 로컬)
    private final AtomicBoolean orchestratorRunning = new AtomicBoolean(false);

    // 스케일아웃 인터럽트 플래그(정보 신호)
    private final AtomicBoolean scaleOutInterruptFlag = new AtomicBoolean(false);

    // 배지 단위 충돌 방지 플래그
    private final Set<String> badgeFlags = ConcurrentHashMap.newKeySet();

    // ===== 전역 API 게이트 =====
    public boolean isGlobalApiGateOn() { return globalApiGate.get(); }
    public void turnOnGlobalApiGate() { globalApiGate.set(true); }
    public void turnOffGlobalApiGate() { globalApiGate.set(false); }

    // ===== 오케스트레이터 런 제어 =====
    public boolean tryStartOrchestratorRun() { return orchestratorRunning.compareAndSet(false, true); }
    public void endOrchestratorRun() { orchestratorRunning.set(false); }
    public boolean isOrchestratorRunning() { return orchestratorRunning.get(); }

    // ===== 스케일아웃 인터럽트 =====
    public void raiseScaleOutInterruptFlag() { scaleOutInterruptFlag.set(true); }
    public boolean isScaleOutInterrupted() { return scaleOutInterruptFlag.get(); }
    public void resetScaleOutInterruptFlag() { scaleOutInterruptFlag.set(false); }

    // ===== 배지 단위 플래그 =====
    public boolean addBadgeFlag(Long userId, Long categoryId) { return badgeFlags.add(key(userId, categoryId)); }
    public void addBadgeFlags(Collection<String> keys) { badgeFlags.addAll(keys); }
    public boolean isBadgeFlagged(Long userId, Long categoryId) { return badgeFlags.contains(key(userId, categoryId)); }
    public void removeBadgeFlag(Long userId, Long categoryId) { badgeFlags.remove(key(userId, categoryId)); }


    public boolean isBadgeBatchRunning() {
        // 전역 게이트가 켜졌거나(클러스터 차단), 현재 프로세스에서 오케스트레이터가 실행 중이면 true
        return isGlobalApiGateOn() || isOrchestratorRunning();
    }

    // 재시작 시 휘발성만 정리
    public void clearTransientFlags() {
        badgeFlags.clear();
        scaleOutInterruptFlag.set(false);
    }

    private String key(Long userId, Long categoryId) { return userId + ":" + categoryId; }
}
