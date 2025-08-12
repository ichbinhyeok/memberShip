// CalcContext.java
package org.example.membership.batch;

import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record CalcContext(
        long rangeStart,
        long rangeEnd,
        int index,
        int total,
        List<User> myUsers,
        Map<Long, Map<Long, OrderCountAndAmount>> statMap,
        Map<String, Boolean> keysToUpdate,
        int batchSize,
        boolean empty
) {
    public static CalcContext of(long s, long e, int idx, int tot,
                                 List<User> users,
                                 Map<Long, Map<Long, OrderCountAndAmount>> stats,
                                 Map<String, Boolean> keys, int batchSize) {
        return new CalcContext(s, e, idx, tot, users, stats, keys, batchSize, false);
    }
    public static CalcContext empty(long s, long e, int idx, int tot) {
        return new CalcContext(s, e, idx, tot, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), 0, true);
    }
}
