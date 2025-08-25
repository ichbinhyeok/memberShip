package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.BatchResultStatus;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "badge_results",
        indexes = {
                @Index(name = "idx_badge_results_exec_status_id", columnList = "execution_id, status, id")
        }
)
public class BadgeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)") // 키셋 페이지 기준 컬럼을 BINARY(16)로 고정
    private UUID id;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.BINARY)
    @Column(name="execution_id", columnDefinition="BINARY(16)", nullable=false)
    private UUID executionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private boolean newState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchResultStatus status = BatchResultStatus.PENDING;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Builder
    public BadgeResult(UUID executionId, Long userId, Long categoryId, boolean newState) {
        this.executionId = executionId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.newState = newState;
    }

    public boolean isNewState() {
        return newState;
    }
}
