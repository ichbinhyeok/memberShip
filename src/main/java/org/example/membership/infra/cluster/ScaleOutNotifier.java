package org.example.membership.infra.cluster;

import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.dto.ScaleOutAckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScaleOutNotifier {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public void notifyOthers(List<WasInstance> others) throws Exception {
        List<CompletableFuture<Void>> futures = others.stream()
                .map(was -> CompletableFuture.runAsync(() -> sendNotification(was), executorService))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new RuntimeException("일부 WAS 인스턴스에 알림 실패", e.getCause());
        }
    }

    private void sendNotification(WasInstance was) {
        String url = "http://" + was.getIp() + ":" + was.getPort() + "/notify-scaleout";

        try {
            ResponseEntity<ScaleOutAckResponse> response = restTemplate.postForEntity(url, null, ScaleOutAckResponse.class);
            ScaleOutAckResponse body = response.getBody();

            if (body != null && body.ack()) {
                log.info("알림 성공: {} - {}", url, body.message());
            } else {
                String message = (body != null) ? body.message() : "응답 없음";
                log.error("알림 실패 (NACK 수신): {} - {}", url, message);
                throw new RuntimeException("NACK: " + message);
            }
        } catch (Exception e) {
            log.error("알림 실패: {} - {}", url, e.getMessage());
            throw new RuntimeException("알림 실패: " + url, e);
        }
    }
}
