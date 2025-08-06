package org.example.membership.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Component
public class MyWasInstanceHolder {

    private UUID myUuid;
    private int myIndex;
    private int totalWases;

    public void setMyUuid(UUID uuid) {
        this.myUuid = uuid;
    }

    public void setMyIndex(int index) {
        this.myIndex = index;
    }

    public void setTotalWases(int total) {
        this.totalWases = total;
    }

    public boolean isMyUser(Long userId) {
        if (userId == null) return true;
        return userId % totalWases == myIndex;
    }

    public boolean isClusterChanged() {
        // 예: 헬스 체크 감지 후 total 변화 확인 (추후 확장 가능)
        return false;
    }
}

