package org.example.membership.common.concurrent;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FlagManager {

    private final AtomicBoolean globalBadgeBatchFlag = new AtomicBoolean(false);
    private final Set<String> badgeFlags = ConcurrentHashMap.newKeySet();

    public void startGlobalBadgeBatch() {
        globalBadgeBatchFlag.set(true);
    }

    public void endGlobalBadgeBatch() {
        globalBadgeBatchFlag.set(false);
    }

    public boolean isBadgeBatchRunning() {
        return globalBadgeBatchFlag.get();
    }

    public boolean addBadgeFlag(Long userId, Long categoryId) {
        return badgeFlags.add(key(userId, categoryId));
    }

    public void addBadgeFlags(Collection<String> keys) {
        badgeFlags.addAll(keys);
    }

    public boolean isBadgeFlagged(Long userId, Long categoryId) {
        return badgeFlags.contains(key(userId, categoryId));
    }

    public void removeBadgeFlag(Long userId, Long categoryId) {
        badgeFlags.remove(key(userId, categoryId));
    }

    public void clearAllFlags() {
        globalBadgeBatchFlag.set(false);
        badgeFlags.clear();
    }

    private String key(Long userId, Long categoryId) {
        return userId + ":" + categoryId;
    }
}
