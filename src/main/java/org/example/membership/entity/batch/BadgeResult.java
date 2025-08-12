package org.example.membership.entity.batch;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.BatchResultStatus;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "badge_results")
public class BadgeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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

    @Builder
    public BadgeResult(UUID executionId, Long userId, Long categoryId, boolean newState) {
        this.executionId = executionId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.newState = newState;



    }
}
