package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.common.enums.MembershipLevel;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "level_results",
        indexes = {
                @Index(name = "idx_level_results_exec_status_id", columnList = "execution_id, status, id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LevelResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.BINARY)
    @Column(name="execution_id", columnDefinition="BINARY(16)", nullable=false)
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

    public void markApplied() {
        this.status = BatchResultStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = BatchResultStatus.FAILED;
        this.appliedAt = LocalDateTime.now();
    }
}
