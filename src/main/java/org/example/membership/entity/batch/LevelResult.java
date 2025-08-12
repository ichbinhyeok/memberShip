package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.common.enums.MembershipLevel;

import java.util.UUID;

// 등급 계산 결과를 저장할 엔티티
@Entity
@Table(name = "level_results")
@Getter
@Setter
@NoArgsConstructor
public class LevelResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Builder
    public LevelResult(UUID executionId, Long userId, MembershipLevel newLevel) {
        this.executionId = executionId;
        this.userId = userId;
        this.newLevel = newLevel;
    }
}