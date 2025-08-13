package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.*;
import org.example.membership.entity.WasInstance;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_execution_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BatchExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, unique = true, updatable = false)
    private UUID executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "was_id", insertable = false, updatable = false)
    private WasInstance wasInstance;

    @Column(nullable = false)
    private String targetDate;

    @Column(name = "cutoff_at")
    private LocalDateTime cutoffAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(nullable = false)
    private boolean interruptedByScaleOut;

    private LocalDateTime interruptedAt;

    private LocalDateTime restoredAt;

    @CreationTimestamp
    private LocalDateTime startedAt;

    @UpdateTimestamp
    private LocalDateTime endedAt;

    public static BatchExecutionLog create(UUID executionId,
                                           LocalDate targetDate,
                                           LocalDateTime cutoffAt,
                                           BatchStatus status) {
        return BatchExecutionLog.builder()
                .executionId(executionId)
                .targetDate(targetDate.toString())
                .cutoffAt(cutoffAt)
                .status(status)
                .startedAt(LocalDateTime.now())
                .build();
    }


    // --- 상태 메서드 ---
    /** 스케일아웃으로 인한 중단 */
    public void markInterruptedByScaleOut() {
        this.status = BatchStatus.INTERRUPTED;
        this.interruptedByScaleOut = true;
        this.interruptedAt = LocalDateTime.now();
    }

    /** 일반 실패로 인한 중단 */
    public void markFailed() {
        this.status = BatchStatus.INTERRUPTED;
        this.interruptedByScaleOut = false;
        this.interruptedAt = LocalDateTime.now();
    }

    /** 복원 상태로 변경 */
    public void markRestoring() {
        this.status = BatchStatus.RESTORING;
        this.restoredAt = LocalDateTime.now();
    }

    /** 정상 완료 */
    public void markCompleted() {
        this.status = BatchStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    /** 실행 중 상태 */
    public void markRunning() {
        this.status = BatchStatus.RUNNING;
    }

    public enum BatchStatus {
        PENDING,
        RUNNING,
        INTERRUPTED,
        RESTORING,
        COMPLETED
    }
}
