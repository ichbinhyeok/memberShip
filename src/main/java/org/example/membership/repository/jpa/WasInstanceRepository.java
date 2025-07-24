package org.example.membership.repository.jpa;

import org.example.membership.entity.WasInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WasInstanceRepository extends JpaRepository<WasInstance, String> {

    @Query("SELECT COUNT(w) FROM WasInstance w WHERE w.status = 'RUNNING'")
    long countRunningInstances();
}
