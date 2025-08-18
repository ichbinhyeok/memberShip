package org.example.membership.entity; // (패키지는 기존 구조에 맞게 조정하세요)

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badge_log")
@Getter
@Setter
@NoArgsConstructor
public class BadgeLog {

    @Id
    private UUID id;

    @Column(name = "badge_id", nullable = false)
    private Long badgeId;

    // 변경 전의 active 상태를 기록
    @Column(name = "previous_active_status", nullable = false)
    private boolean previousActiveStatus;

    // 변경이 발생한 시간 (T0 시점과 비교하기 위함)
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public BadgeLog(Long badgeId, boolean previousActiveStatus) {
        this.badgeId = badgeId;
        this.previousActiveStatus = previousActiveStatus;
        this.changedAt = LocalDateTime.now();
    }
}