package org.example.membership.common.util;


public class WasPartitionUtil {

    // 전체 WAS 개수와 이 WAS의 인덱스를 설정
    public static final int TOTAL_WAS = 2; // 일단 하드 코딩
    public static final int CURRENT_INDEX = 0; // was1이면 0, was2면 1

    // 유저 ID 기반으로 이 WAS가 처리할지 여부 판단
    public static boolean isResponsibleFor(Long userId) {
        return (userId % TOTAL_WAS) == CURRENT_INDEX;
    }
}
