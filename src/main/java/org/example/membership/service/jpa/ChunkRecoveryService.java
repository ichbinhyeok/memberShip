package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.batch.ChunkExecutionLog;
import org.example.membership.repository.jpa.batch.ChunkExecutionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChunkRecoveryService {

    private final ChunkExecutionLogRepository chunkExecutionLogRepository;

    @Transactional
    public void processChunk(Long chunkId) {
        ChunkExecutionLog chunk = chunkExecutionLogRepository.findById(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found"));
        chunk.setRestored(true);
        chunkExecutionLogRepository.save(chunk);
    }
}