package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "step_execution_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"target_date", "was_index"})
})
@Getter
@Setter
@NoArgsConstructor
public class StepExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate targetDate;
    private int wasIndex;

    private boolean badgeDone = false;
    private boolean levelDone = false;
    private boolean couponDone = false;

    private LocalDateTime updatedAt;
}
