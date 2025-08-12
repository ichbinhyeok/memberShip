package org.example.membership.repository.jpa.batch;

import org.example.membership.entity.batch.StepExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StepExecutionLogRepository extends JpaRepository<StepExecutionLog, Long> {

    Optional<StepExecutionLog> findByTargetDateAndWasIndex(LocalDate date, int wasIndex);
}

