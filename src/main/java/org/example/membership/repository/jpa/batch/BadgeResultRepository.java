package org.example.membership.repository.jpa.batch;

import org.example.membership.entity.batch.BadgeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BadgeResultRepository extends JpaRepository<BadgeResult, UUID> {

    @Query(value = """
    SELECT id, execution_id, user_id, category_id, new_state, status, applied_at
    FROM badge_results
    WHERE execution_id = :exec
      AND status = 'PENDING'
      AND (:afterId IS NULL OR id > :afterId)
    ORDER BY id
    LIMIT :limit
    """, nativeQuery = true)
    List<BadgeResult> findPendingAfterId(@Param("exec") UUID exec,
                                            @Param("afterId") UUID afterId,
                                            @Param("limit") int limit);

    @Modifying
    @Query(value = """
    UPDATE badge_results
    SET status = 'APPLIED', applied_at = NOW()
    WHERE id IN (:ids)
    """, nativeQuery = true)
    int markApplied(@Param("ids") List<UUID> ids);

}


