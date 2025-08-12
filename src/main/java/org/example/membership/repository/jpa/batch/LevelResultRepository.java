package org.example.membership.repository.jpa.batch;

import org.example.membership.common.enums.BatchResultStatus;
import org.example.membership.entity.batch.LevelResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface LevelResultRepository extends JpaRepository<LevelResult,Long> {
    List<LevelResult> findByExecutionIdAndStatus(UUID executionId, BatchResultStatus status);

    @Transactional
    @Modifying
    void deleteByExecutionId(UUID executionId);
}

