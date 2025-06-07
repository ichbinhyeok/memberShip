package org.example.membership.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@Service
@RequiredArgsConstructor
@Slf4j
public class FastDataGenerationService {

    private final DataSource dataSource;
    private static final int USER_BATCH_SIZE = 100000;
    private static final int ORDER_BATCH_SIZE = 100000;

    private static final String[] LAST_NAMES = { "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "류", "전", "홍", "고", "문", "양", "손", "배", "조", "백", "허", "유" };
    private static final String[] FIRST_NAMES = { "민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "지후", "준서", "서진", "은우", "현우", "연우", "정우", "승우", "시원", "민재", "현준", "원준", "지원", "서현", "서윤", "지우", "하은", "민서", "윤서", "수아", "소율", "지안", "채원", "예원", "유나", "서아", "다은", "예은", "시은", "하린", "연서", "수빈", "영희", "철수", "영수", "순자", "미영", "정호", "승현", "태현", "진우", "상훈" };

    public void generateUsers(int count) {
        log.info("🔄 사용자 {}명 생성 시작 (JDBC)", count);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name, membership_level, created_at) VALUES (?, ?, ?)")) {
            conn.setAutoCommit(false);

            for (int i = 0; i < count; i++) {
                ps.setString(1, generateRandomName());
                ps.setString(2, "SILVER");
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(30))));
                ps.addBatch();

                if (i % USER_BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                    log.info("✅ 사용자 진행률: {}/{}", i + 1, count);
                }
            }

            ps.executeBatch();
            conn.commit();
            log.info("🎉 사용자 생성 완료");

        } catch (SQLException e) {
            throw new RuntimeException("사용자 배치 삽입 실패", e);
        }
    }

    public void generateOrders(int count) {
        log.info("🔄 주문 {}건 생성 시작 (JDBC)", count);

        List<Long> userIds = getAllUserIds();
        if (userIds.isEmpty()) throw new IllegalStateException("유저가 없습니다.");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO orders (user_id, order_amount, status, ordered_at) VALUES (?, ?, ?, ?)")) {
            conn.setAutoCommit(false);
            Random rand = new Random();

            for (int i = 0; i < count; i++) {
                ps.setLong(1, userIds.get(rand.nextInt(userIds.size())));
                ps.setBigDecimal(2, generateRandomAmount());
                ps.setString(3, getRandomOrderStatus());
                ps.setTimestamp(4, Timestamp.valueOf(generateRandomDateInMay()));
                ps.addBatch();

                if (i % ORDER_BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                    log.info("✅ 주문 진행률: {}/{}", i + 1, count);
                }
            }

            ps.executeBatch();
            conn.commit();
            log.info("🎉 주문 생성 완료");

        } catch (SQLException e) {
            throw new RuntimeException("주문 배치 삽입 실패", e);
        }
    }

    private List<Long> getAllUserIds() {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM users");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("유저 ID 조회 실패", e);
        }
        return ids;
    }

    private String generateRandomName() {
        return LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)] +
                FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
    }

    private BigDecimal generateRandomAmount() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(10, 1501) * 100);
    }

    private String getRandomOrderStatus() {
        int r = ThreadLocalRandom.current().nextInt(100);
        return (r < 85) ? "PAID" : (r < 95) ? "CANCELLED" : "REFUNDED";
    }

    private LocalDateTime generateRandomDateInMay() {
        return LocalDateTime.of(2025, 5,
                ThreadLocalRandom.current().nextInt(1, 32),
                ThreadLocalRandom.current().nextInt(0, 24),
                ThreadLocalRandom.current().nextInt(0, 60),
                ThreadLocalRandom.current().nextInt(0, 60));
    }
}
