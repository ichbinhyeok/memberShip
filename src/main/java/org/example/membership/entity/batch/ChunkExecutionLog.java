package org.example.membership.entity.batch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chunk_execution_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType stepType;


    //어떤 배치의 청크인지 → 복원 기준의 핵심 키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_execution_id", nullable = false)
    private BatchExecutionLog batchExecutionLog;

    //어떤 WAS가 이 청크를 실행했는가
    // WAS UUID 구분 (중단된 WAS인지 식별 목적)
    @Column(nullable = false)
    private UUID wasId;

    // 이 청크는 언제 기록됐는가
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    // 실제 처리 범위
    @Column(nullable = false)
    private Long userIdStart;

    @Column(nullable = false)
    private Long userIdEnd;

    // 처리 성공 여부 (flush 직전까지 처리했는가)
    @Column(nullable = false)
    private boolean completed;

    // 복원 시 중복 처리 피하기 위해 구분
    private boolean restored;

    public enum StepType {
        BADGE, LEVEL, COUPON
    }

}
