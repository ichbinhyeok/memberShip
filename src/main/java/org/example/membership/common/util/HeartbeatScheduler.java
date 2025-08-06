package org.example.membership.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

    private final WasInstanceRepository wasInstanceRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @Transactional //안하면 save(was)하고 flush를 제어할 수 없어서 정합성이 깨질 수 있음.
    @Scheduled(fixedRate = 10_000)//10초에 한 번씩 하트비트
    public void sendHeartbeat() {

        UUID myId = myWasInstanceHolder.getMyUuid();

        if (myId == null) {
            log.warn("[Heartbeat] UUID가 아직 초기화되지 않아 스킵합니다.");
            return;
        }
        wasInstanceRepository.findById(myId).ifPresent(was -> {
            was.updateHeartbeat();
            wasInstanceRepository.save(was);
        });
    }
}
