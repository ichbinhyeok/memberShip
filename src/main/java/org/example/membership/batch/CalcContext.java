package org.example.membership.batch;

import org.example.membership.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record CalcContext(
        List<User> myUsers,
        Map<String, Boolean> keysToUpdate,
        int batchSize,
        boolean empty
) {
    /**
     * 정상적인 계산 컨텍스트 생성
     */
    public static CalcContext of(List<User> users,
                                 Map<String, Boolean> keys,
                                 int batchSize) {
        return new CalcContext(users, keys, batchSize, false);
    }

    /**
     * 빈 컨텍스트 생성 (복원 대상 없음, 계산할 사용자 없음 등)
     */
    public static CalcContext createEmpty() {
        return new CalcContext(Collections.emptyList(), Collections.emptyMap(), 0, true);
    }
}
