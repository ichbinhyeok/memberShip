
package org.example.membership.infra.cluster;

import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.WasInstance;
import org.example.membership.infra.cluster.dto.ScaleOutAckResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class ScaleOutNotifier {

    private final WebClient webClient = WebClient.builder().build();

    public void notifyOthers(List<WasInstance> others) {
        for (WasInstance was : others) {
            String url = "http://" + was.getIp() + ":" + was.getPort() + "/notify-scaleout";

            webClient.post()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ScaleOutAckResponse.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryable)
                            .doBeforeRetry(retrySignal -> log.info("[재시도] {} (시도 #{})", url, retrySignal.totalRetries() + 1))
                    )
                    .doOnNext(resp -> {
                        if (resp.ack()) {
                            log.info(" 알림 성공: {} - {}", url, resp.message());
                        } else {
                            log.warn("️ 알림 응답 실패: {} - {}", url, resp.message());
                        }
                    })
                    .doOnError(error -> log.warn(" 알림 최종 실패: {} - {}", url, error.getMessage()))
                    .subscribe();
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError()
                    || ex.getStatusCode().value() == 404;
        }
        return true;
    }
}
