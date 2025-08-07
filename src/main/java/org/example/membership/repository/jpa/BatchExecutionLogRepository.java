package org.example.membership.repository.jpa;

import org.example.membership.entity.BatchExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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


    @Query("""
    SELECT b
    FROM BatchExecutionLog b
    WHERE b.status = 'INTERRUPTED'
      AND b.interruptedByScaleOut = true
    """)
    List<BatchExecutionLog> findRestorableBatches();

    @Modifying
    @Query("""
    UPDATE BatchExecutionLog b
       SET b.status = 'RESTORING',
           b.restoredAt = :now
     WHERE b.id = :id
       AND b.status = 'INTERRUPTED'
    """)
    int updateStatusToRestoring(@Param("id") Long id, @Param("now") LocalDateTime now);
}
