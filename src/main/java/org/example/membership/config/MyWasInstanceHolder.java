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
@RequiredArgsConstructor
public class MyWasInstanceHolder {

    private final WasInstanceRepository wasInstanceRepository;
    private UUID myUuid;    // 초기화 없이 set으로 주입
    private int myIndex;    // 초기화 없이 set으로 주입

    public void setMyUuid(UUID uuid) {
        this.myUuid = uuid;
    }

    public void setMyIndex(int index) {
        this.myIndex = index;
    }

    public int getTotalWas() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        return wasInstanceRepository.findAliveInstances(threshold).size();
    }

    public boolean isMyUser(Long userId) {
        if (userId == null) return true;
        return userId % getTotalWas() == myIndex;
    }
}
