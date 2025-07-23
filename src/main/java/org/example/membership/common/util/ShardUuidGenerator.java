package org.example.membership.common.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ShardUuidGenerator {

    private ShardUuidGenerator() {
    }

    private static final AtomicInteger counter = new AtomicInteger();

    public static UUID generate(int shardNo) {
        if (shardNo < 0 || shardNo > 99) {
            throw new IllegalArgumentException("shardNo must be between 0 and 99");
        }

        long timestamp = System.currentTimeMillis();
        int seq = counter.getAndUpdate(i -> (i + 1) % 1000);
        String base = String.format("%013d%02d%03d", timestamp, shardNo, seq);
        return UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }
}
