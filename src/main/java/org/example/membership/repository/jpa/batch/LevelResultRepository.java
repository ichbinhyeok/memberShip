// LevelResultRepository.java
package org.example.membership.repository.jpa.batch;

import org.example.membership.entity.batch.LevelResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LevelResultRepository extends JpaRepository<LevelResult, UUID> {

    @Query(value = """
    SELECT id, execution_id, user_id, new_level, status, applied_at
    FROM level_results
    WHERE execution_id = :exec
      AND status = 'PENDING'
      AND (:afterId IS NULL OR id > :afterId)
    ORDER BY id
    LIMIT :limit
    """, nativeQuery = true)
    List<LevelResult> findPendingAfterId(@Param("exec") UUID exec,
                                            @Param("afterId") UUID afterId,
                                            @Param("limit") int limit);
}
