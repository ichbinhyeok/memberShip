package org.example.membership.common.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public class ShardPrefixedUuidGenerator {

    public static String generate(int shardNo) {
        if (shardNo < 0 || shardNo > 99) {
            throw new IllegalArgumentException("shardNo must be between 0 and 99");
        }
        UUID uuid = UuidCreator.getTimeOrdered(); // 빠르고 정렬 가능한 UUID
        return String.format("%02d_%s", shardNo, uuid.toString());
    }
}

