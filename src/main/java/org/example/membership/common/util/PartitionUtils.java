package org.example.membership.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartitionUtils {
    public static <T> List<List<T>> partition(List<T> list, int groupCount) {
        if (list == null || groupCount <= 0) {
            return Collections.emptyList();
        }
        int total = list.size();
        int chunkSize = (int) Math.ceil(total / (double) groupCount);
        List<List<T>> result = new ArrayList<>(groupCount);
        int start = 0;
        for (int i = 0; i < groupCount; i++) {
            int end = Math.min(start + chunkSize, total);
            if (start >= total) {
                result.add(Collections.emptyList());
            } else {
                result.add(new ArrayList<>(list.subList(start, end)));
            }
            start = end;
        }
        return result;
    }
}