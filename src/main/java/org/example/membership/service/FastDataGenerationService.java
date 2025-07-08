package org.example.membership.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.CategoryType;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.common.enums.OrderStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class FastDataGenerationService {

    private final DataSource dataSource;
    private static final int USER_BATCH_SIZE = 10000;
    private static final int ORDER_BATCH_SIZE = 10000;
    private static final int PRODUCT_BATCH_SIZE = 1000;
    private static final int CATEGORY_BATCH_SIZE = 100;
    private static final int COUPON_BATCH_SIZE = 100;

    private static final String[] LAST_NAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신", "권", "황", "안", "송", "류", "전", "홍", "고", "문", "양", "손", "배", "조", "백", "허", "유"};
    private static final String[] FIRST_NAMES = {"민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "지후", "준서", "서진", "은우", "현우", "연우", "정우", "승우", "시원", "민재", "현준", "원준", "지원", "서현", "서윤", "지우", "하은", "민서", "윤서", "수아", "소율", "지안", "채원", "예원", "유나", "서아", "다은", "예은", "시은", "하린", "연서", "수빈", "영희", "철수", "영수", "순자", "미영", "정호", "승현", "태현", "진우", "상훈"};

    public void generateAll() {
        int categoryCount = generateCategories();
        int productCount = generateProducts();
        int userCount = generateUsers(30000);
        int badgeCount = generateBadgeSkeletons();
        int couponCount = generateCouponPoliciesByCategory();
        int orderCount = generateOrdersForUsers();
        log.info("🎉 전체 데이터 생성 완료 - categories: {}, coupons: {}, products: {}, users: {}, badges: {}, orders: {}",
                categoryCount, couponCount, productCount, userCount, badgeCount, orderCount);
    }

    private int generateUsers(int count) {
        log.info("🔄 사용자 {}명 생성 시작 (JDBC)", count);
        MembershipLevel[] levels = MembershipLevel.values();
        int inserted = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name, membership_level, created_at) VALUES (?, ?, ?)") ) {
            conn.setAutoCommit(false);
            for (int i = 0; i < count; i++) {
                ps.setString(1, generateRandomName());
                ps.setString(2, levels[ThreadLocalRandom.current().nextInt(levels.length)].name());
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(30))));
                ps.addBatch();
                inserted++;
                if (inserted % USER_BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                    log.info("✅ 사용자 진행률: {}/{}", inserted, count);
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 사용자 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("사용자 배치 삽입 실패", e);
        }
        return inserted;
    }

    private int generateBadgeSkeletons() {
        List<Long> userIds = getAllUserIds();
        List<Long> categoryIds = getAllCategoryIds();
        if (userIds.isEmpty() || categoryIds.isEmpty()) {
            return 0;
        }
        log.info("🔄 배지 스켈레톤 생성 시작 (JDBC)");
        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO badges (user_id, category_id, active, updated_at) VALUES (?, ?, ?, ?)") ) {
            conn.setAutoCommit(false);
            for (Long uId : userIds) {
                for (Long cId : categoryIds) {
                    ps.setLong(1, uId);
                    ps.setLong(2, cId);
                    ps.setBoolean(3, false);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.addBatch();
                    total++;
                    if (total % USER_BATCH_SIZE == 0) {
                        ps.executeBatch();
                        ps.clearBatch();
                        log.info("✅ 배지 진행률: {}", total);
                    }
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 배지 스켈레톤 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("배지 배치 삽입 실패", e);
        }
        return total;
    }

    private int generateOrdersForUsers() {
        List<Long> userIds = getAllUserIds();
        Map<Long, BigDecimal> productPriceMap = getProductPriceMap();
        List<Long> productIds = new ArrayList<>(productPriceMap.keySet());
        if (userIds.isEmpty() || productIds.isEmpty()) {
            return 0;
        }
        log.info("🔄 전체 사용자 주문 생성 시작 (JDBC)");
        int total = 0;
        Random rand = new Random();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO orders (user_id, product_id, order_amount, status, ordered_at) VALUES (?, ?, ?, ?, ?)") ) {
            conn.setAutoCommit(false);
            for (Long userId : userIds) {
                int orderCount = rand.nextInt(11) + 30; // 10~20
                for (int i = 0; i < orderCount; i++, total++) {
                    Long productId = productIds.get(rand.nextInt(productIds.size()));
                    BigDecimal price = productPriceMap.get(productId);
                    int qty = rand.nextInt(5) + 1;
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));
                    ps.setLong(1, userId);
                    ps.setLong(2, productId);
                    ps.setBigDecimal(3, amount);
                    ps.setString(4, getRandomOrderStatus());
                    int month = rand.nextInt(3) + 3; // 3~5월
                    ps.setTimestamp(5, Timestamp.valueOf(generateRandomDate(2025, month)));
                    ps.addBatch();
                    if (total % ORDER_BATCH_SIZE == 0) {
                        ps.executeBatch();
                        ps.clearBatch();
                        log.info("✅ 주문 진행률: {}", total);
                    }
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 주문 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("주문 배치 삽입 실패", e);
        }
        return total;
    }

    private int generateProducts() {
        List<Long> categoryIds = getAllCategoryIds();
        log.info("🔄 상품 생성 시작 (JDBC)");
        int inserted = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)") ) {
            conn.setAutoCommit(false);
            for (Long catId : categoryIds) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(1, "상품" + catId + "-" + i);
                    ps.setBigDecimal(2, generateRandomPrice());
                    ps.setLong(3, catId);
                    ps.addBatch();
                    inserted++;
                    if (inserted % PRODUCT_BATCH_SIZE == 0) {
                        ps.executeBatch();
                        ps.clearBatch();
                        log.info("✅ 상품 진행률: {}", inserted);
                    }
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 상품 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("상품 배치 삽입 실패", e);
        }
        return inserted;
    }

    private int generateCategories() {
        CategoryType[] types = CategoryType.values();
        log.info("🔄 카테고리 {}건 생성 시작 (JDBC)", types.length);
        int inserted = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO categories(name) VALUES (?)")) {
            conn.setAutoCommit(false);
            for (CategoryType type : types) {
                ps.setString(1, type.name());
                ps.addBatch();
                inserted++;
                if (inserted % CATEGORY_BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                    log.info("✅ 카테고리 진행률: {}/{}", inserted, types.length);
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 카테고리 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("카테고리 배치 삽입 실패", e);
        }
        return inserted;
    }

    private int generateCouponPoliciesByCategory() {
        List<Long> categoryIds = getAllCategoryIds();
        if (categoryIds.isEmpty()) {
            return 0;
        }
        log.info("🔄 카테고리별 쿠폰 정책 생성 시작 (JDBC)");
        int inserted = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
             INSERT INTO coupons (category_id, discount_amount,code)
             VALUES (?, ?,?)
        """)) {
            conn.setAutoCommit(false);
            for (Long catId : categoryIds) {
                ps.setLong(1, catId);
                ps.setBigDecimal(2, new BigDecimal("1000.00"));
                ps.setString(3, UUID.randomUUID().toString().substring(0, 8));
                ps.addBatch();

                inserted++;
                if (inserted % CATEGORY_BATCH_SIZE == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                    log.info("✅ 쿠폰 정책 진행률: {}", inserted);
                }
            }
            ps.executeBatch();
            conn.commit();
            log.info("🎉 쿠폰 정책 생성 완료");
        } catch (SQLException e) {
            throw new RuntimeException("쿠폰 정책 배치 삽입 실패", e);
        }
        return inserted;
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

    private Map<Long, BigDecimal> getProductPriceMap() {
        Map<Long, BigDecimal> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, price FROM products");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getLong("id"), rs.getBigDecimal("price"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
        return map;
    }

    private List<Long> getAllCategoryIds() {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM categories");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("카테고리 ID 조회 실패", e);
        }
        return ids;
    }

    private String generateRandomName() {
        return LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)] +
                FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
    }

    private BigDecimal generateRandomPrice() {
        return BigDecimal.valueOf(
                ThreadLocalRandom.current().nextInt(10, 51) * 1000
        );
    }

    private String getRandomOrderStatus() {
        int r = ThreadLocalRandom.current().nextInt(100);
        return (r < 85) ? OrderStatus.PAID.name() : (r < 95) ? OrderStatus.CANCELLED.name() : OrderStatus.REFUNDED.name();
    }

    private LocalDateTime generateRandomDate(int year, int month) {
        int lastDay = java.time.YearMonth.of(year, month).lengthOfMonth();
        return LocalDateTime.of(
                year,
                month,
                ThreadLocalRandom.current().nextInt(1, lastDay + 1),
                ThreadLocalRandom.current().nextInt(0, 24),
                ThreadLocalRandom.current().nextInt(0, 60),
                ThreadLocalRandom.current().nextInt(0, 60)
        );
    }
}
