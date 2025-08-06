package org.example.membership.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.ScaleOutNotifier;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WasInstanceRegistrar implements ApplicationRunner {

    private final WasInstanceRepository wasInstanceRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;
    private final ScaleOutNotifier scaleOutNotifier;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        String hostname = InetAddress.getLocalHost().getHostName();
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));

        UUID uuid = UUID.randomUUID();

        // 1. 내 인스턴스 등록
        WasInstance instance = new WasInstance();
        instance.setId(uuid);
        instance.setIp(ip);
        instance.setHostname(hostname);
        instance.setPort(port);
        instance.setRegisteredAt(LocalDateTime.now());
        instance.setLastHeartbeatAt(LocalDateTime.now());

        wasInstanceRepository.save(instance);

        // 2. 전체 살아있는 인스턴스 조회 + 정렬
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<WasInstance> aliveInstances = wasInstanceRepository.findAliveInstances(threshold)
                .stream()
                .sorted(Comparator.comparing(WasInstance::getRegisteredAt))
                .toList();

        // 3. 내 index 계산 (UUID 기준)
        int myIndex = -1;
        for (int i = 0; i < aliveInstances.size(); i++) {
            if (aliveInstances.get(i).getId().equals(uuid)) {
                myIndex = i;
                break;
            }
        }

        if (myIndex == -1) {
            throw new IllegalStateException("등록한 인스턴스를 정렬 목록에서 찾지 못했습니다.");
        }

        // 4. 메모리에 보관
        myWasInstanceHolder.setMyUuid(uuid);
        myWasInstanceHolder.setMyIndex(myIndex);
        myWasInstanceHolder.setTotalWases(aliveInstances.size());


        // 5. 다른 인스턴스에게 Scale-out 알림 전송
        List<WasInstance> others = aliveInstances.stream()
                .filter(was -> !was.getId().equals(uuid))
                .toList();

        if (!others.isEmpty()) {
            log.info("Scale-out 알림 대상 WAS 수: {}", others.size());
            scaleOutNotifier.notifyOthers(others);
        }
    }
}
