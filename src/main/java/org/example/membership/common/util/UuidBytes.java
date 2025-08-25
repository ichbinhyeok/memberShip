//package org.example.membership.common.util;
//
//import java.nio.ByteBuffer;
//import java.util.UUID;
//
//// Uuid와 byte[]를 상호 변환하는 유틸리티 클래스
//public final class UuidBytes {
//    // 유틸리티 클래스이므로 인스턴스화 방지
//    private UuidBytes() {}
//
//    /**
//     * UUID 객체를 16바이트 크기의 byte 배열로 변환
//     */
//    public static byte[] toBytes(UUID u){
//        // 1. 16바이트 크기의 ByteBuffer를 할당
//        // UUID는 128비트 = 16바이트이므로 크기를 16으로 지정
//        ByteBuffer bb = ByteBuffer.allocate(16);
//
//        // 2. UUID의 앞부분 64비트(8바이트)를 버퍼에 기록
//        bb.putLong(u.getMostSignificantBits());
//
//        // 3. UUID의 뒷부분 64비트(8바이트)를 버퍼에 이어서 기록
//        bb.putLong(u.getLeastSignificantBits());
//
//        // 4. 내용이 모두 채워진 버퍼의 실제 byte 배열을 반환
//        return bb.array();
//    }
//}