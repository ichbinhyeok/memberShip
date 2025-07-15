package org.example.membership.common.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class UserCategoryProcessingFlagManager {
    private final ConcurrentMap<String, Boolean> flags = new ConcurrentHashMap<>();

    public boolean mark(String key) {
        return flags.putIfAbsent(key, Boolean.TRUE) == null;
    }

    public void clear(String key) {
        flags.remove(key);
    }

    public boolean isProcessing(String key) {
        return flags.containsKey(key);
    }
}