package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    // 배치 식별용 UUID (WAS별로 배치 인스턴스를 구분하기 위함)
    @Column(name = "execution_id", nullable = false, unique = true, updatable = false)
    private UUID executionId;


    // 배치를 수행한 WAS UUID
    @Column(name = "was_id", nullable = false)
    private UUID wasId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "was_id", insertable = false, updatable = false)
    private WasInstance wasInstance;

    // 배치 대상 날짜 (ex: '2025-08-01')
    @Column(nullable = false)
    private String targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    // 오토스케일링에 의한 중단 여부
    @Column(nullable = false)
    private boolean interruptedByScaleOut;

    // 인터럽트 시각
    private LocalDateTime interruptedAt;

    // 복원 시작 시각
    private LocalDateTime restoredAt;

    // 실제 배치 시작 시간
    @CreationTimestamp
    private LocalDateTime startedAt;

    // 배치 종료 시간 (중단 or 성공 모두 포함)
    @UpdateTimestamp
    private LocalDateTime endedAt;

    // 상태를 갱신하는 메서드
    public void markInterrupted() {
        this.status = BatchStatus.INTERRUPTED;
        this.interruptedByScaleOut = true;
        this.interruptedAt = LocalDateTime.now();
    }

    public void markRestoring() {
        this.status = BatchStatus.RESTORING;
        this.restoredAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = BatchStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void markRunning() {
        this.status = BatchStatus.RUNNING;
    }

    public enum BatchStatus {
        PENDING,    // 생성만 됐지만 아직 실행 X
        RUNNING,    // 정상 실행 중
        INTERRUPTED,// 인터럽트 상태 (scale-out으로 인한 중단)
        RESTORING,  // 복원 중
        COMPLETED   // 정상 완료
    }
}
