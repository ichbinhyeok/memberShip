package org.example.membership.common.util;


public class ShardUtil {
    private final int total;
    private final int index;

    public ShardUtil(int total, int index) {
        this.total = total;
        this.index = index;
    }

    public boolean isMine(Long userId) {
        return userId != null && userId % total == index;
    }
}
