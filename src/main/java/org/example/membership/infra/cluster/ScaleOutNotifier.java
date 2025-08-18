// org/example/membership/infra/cluster/ScaleOutNotifier.java
package org.example.membership.infra.cluster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.dto.ScaleOutAckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScaleOutNotifier {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService;


    public void notifyOthers(List<WasInstance> others) throws Exception {
        List<CompletableFuture<Void>> futures = others.stream()
                .map(was -> CompletableFuture.runAsync(() -> sendNotification(was), executorService))
                .toList();
        waitAll(futures, "일부 WAS 인스턴스에 알림 실패");
    }

    public void notifyBadgeFlagOffToOthers(List<WasInstance> others) throws Exception {
        List<CompletableFuture<Void>> futures = others.stream()
                .map(was -> CompletableFuture.runAsync(() -> sendNotification(was, "/internal/batch/badge-flag/off"), executorService))
                .toList();
        waitAll(futures, "일부 WAS 인스턴스에 배지 플래그 해제 실패");
    }


    public void notifyBadgeFlagOnToOthers(List<WasInstance> others) throws Exception {
        List<CompletableFuture<Void>> futures = others.stream()
                .map(was -> CompletableFuture.runAsync(() -> sendNotification(was, "/internal/batch/badge-flag/on"), executorService))
                .toList();
        waitAll(futures, "일부 WAS 인스턴스에 배지 플래그 설정 실패");
    }


    private void sendNotification(WasInstance was) {
        sendNotification(was, "/notify-scaleout");
    }

    private void sendNotification(WasInstance was, String path) {
        String url = "http://" + was.getIp() + ":" + was.getPort() + path;
        try {
            ResponseEntity<ScaleOutAckResponse> response =
                    restTemplate.postForEntity(url, null, ScaleOutAckResponse.class);
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

    private static void waitAll(List<CompletableFuture<Void>> futures, String failMsg) throws Exception {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new RuntimeException(failMsg, e.getCause());
        }
    }
}
