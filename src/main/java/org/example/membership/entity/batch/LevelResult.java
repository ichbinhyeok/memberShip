package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 등급 계산 결과를 저장하는 엔티티
 * - 배치 실행(executionId) 단위로 산출된 등급을 저장
 * - PENDING → APPLIED / FAILED 상태로 전환
 */
@Entity
@Table(name = "level_results")
@Getter
@Setter
@NoArgsConstructor
public class LevelResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID executionId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipLevel newLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchResultStatus status = BatchResultStatus.PENDING;

    @Column
    private LocalDateTime appliedAt;

    @Builder
    public LevelResult(UUID executionId, Long userId, MembershipLevel newLevel) {
        this.executionId = executionId;
        this.userId = userId;
        this.newLevel = newLevel;
    }

    /** 적용 시점 기록 */
    public void markApplied() {
        this.status = BatchResultStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
    }

    /** 실패 시 기록 */
    public void markFailed() {
        this.status = BatchResultStatus.FAILED;
        this.appliedAt = LocalDateTime.now();
    }
}
