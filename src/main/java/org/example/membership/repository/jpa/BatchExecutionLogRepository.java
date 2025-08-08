package org.example.membership.repository.jpa;

import org.example.membership.entity.BatchExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.membership.entity.BatchExecutionLog.BatchStatus;


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


    @Query("""
      select count(b) from BatchExecutionLog b
      where b.targetDate = :targetDate
        and b.status in (:s1, :s2)
    """)
    long countActiveForTargetDate(@Param("targetDate") String targetDate,
                                  @Param("s1") BatchStatus s1,
                                  @Param("s2") BatchStatus s2);

    @Query("""
      select b from BatchExecutionLog b
      where b.targetDate = :targetDate
        and b.status = :status
        and b.interruptedByScaleOut = true
      order by b.startedAt asc
    """)
    List<BatchExecutionLog> findInterruptedScaleOut(@Param("targetDate") String targetDate,
                                                    @Param("status") BatchStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      update BatchExecutionLog b
      set b.status = 'RESTORING', b.restoredAt = CURRENT_TIMESTAMP
      where b.id = :id
        and b.status = 'INTERRUPTED'
        and b.interruptedByScaleOut = true
    """)
    int tryAcquireRestore(@Param("id") Long id);

    // 필요 시: 하트비트까지 함께 보고 싶을 때
    @Query("""
      select count(b) from BatchExecutionLog b
      join b.wasInstance w
      where b.targetDate = :targetDate
        and b.status in (:s1, :s2)
        and w.lastHeartbeatAt >= :threshold
    """)
    long countActiveWithAliveHeartbeat(@Param("targetDate") String targetDate,
                                       @Param("s1") BatchStatus s1,
                                       @Param("s2") BatchStatus s2,
                                       @Param("threshold") LocalDateTime threshold);


}



