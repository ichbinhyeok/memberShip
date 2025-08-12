package org.example.membership.batch;

public interface ScaleOutGuard {
    /** 배치 시작 시 기대 WAS 수를 설정하고, 즉시 1회 폴링합니다. */
    void init(long expectedTotal);

    /** 청크 저장 직후 등, 점검 지점마다 호출하십시오. 불일치 시 예외를 던집니다. */
    void ensureUnchanged();

    /** 최근 폴링 기준 현재 살아있는 WAS 수(캐시)를 제공합니다. */
    long currentAlive();
}
