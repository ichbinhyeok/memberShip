package org.example.membership.repository.jpa;

import org.example.membership.entity.WasInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WasInstanceRepository extends JpaRepository<WasInstance, UUID> {

    @Query("SELECT COUNT(w) FROM WasInstance w WHERE w.status = 'RUNNING'")
    long countRunningInstances();

    @Query("SELECT w FROM WasInstance w WHERE w.lastHeartbeatAt >= :threshold")
    List<WasInstance> findAliveInstances(@Param("threshold") LocalDateTime threshold);

}
