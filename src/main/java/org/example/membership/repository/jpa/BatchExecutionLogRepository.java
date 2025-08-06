package org.example.membership.repository.jpa;

import org.example.membership.entity.BatchExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;

public interface BatchExecutionLogRepository extends JpaRepository<BatchExecutionLog,Long> {

    @Query("SELECT COUNT(b) FROM BatchExecutionLog b WHERE b.status = 'RUNNING'")
    long countRunningBatches();

    @Query("""
    SELECT COUNT(b)
    FROM BatchExecutionLog b
    JOIN b.wasInstance w
    WHERE b.status = 'RUNNING'
      AND w.lastHeartbeatAt > :threshold
    """)
    long countRunningWithAliveHeartbeat(@Param("threshold") LocalDateTime threshold);

}
