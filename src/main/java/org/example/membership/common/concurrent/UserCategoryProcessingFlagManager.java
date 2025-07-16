package org.example.membership.common.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UserCategoryProcessingFlagManager {
    private final ConcurrentMap<String, Boolean> flags = new ConcurrentHashMap<>();
    private final AtomicBoolean badgeBatchRunning = new AtomicBoolean(false);

    public boolean mark(String key) {
        return flags.putIfAbsent(key, Boolean.TRUE) == null;
    }

    public void clear(String key) {
        flags.remove(key);
    }

    public boolean isProcessing(String key) {
        return flags.containsKey(key);
    }

    public boolean markBadgeBatchRunning() {
        return badgeBatchRunning.compareAndSet(false, true);
    }

    public void clearBadgeBatchRunning() {
        badgeBatchRunning.set(false);
    }

    public boolean isBadgeBatchRunning() {
        return badgeBatchRunning.get();
    }
}