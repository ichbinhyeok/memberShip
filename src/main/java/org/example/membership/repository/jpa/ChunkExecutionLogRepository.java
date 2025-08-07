package org.example.membership.repository.jpa;

import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChunkExecutionLogRepository extends JpaRepository<ChunkExecutionLog, Long> {

    List<ChunkExecutionLog> findByBatchExecutionLogAndRestoredFalseOrderByUserIdStartAsc(BatchExecutionLog batchExecutionLog);

    @Query("""
    SELECT c FROM ChunkExecutionLog c
    WHERE c.batchExecutionLog.executionId = :executionId
      AND c.stepType = :stepType
      AND c.completed = true
""")
    List<ChunkExecutionLog> findCompletedChunks(
            @Param("executionId") UUID executionId,
            @Param("stepType") ChunkExecutionLog.StepType stepType
    );

}
