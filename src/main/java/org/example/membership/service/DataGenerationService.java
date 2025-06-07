package org.example.membership.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.domain.order.Order;
import org.example.membership.domain.order.jpa.OrderRepository;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataGenerationService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private static final int USER_BATCH_SIZE = 5000;
    private static final int ORDER_BATCH_SIZE = 100000;

    private static final String[] LAST_NAMES = {
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "전",
            "홍", "고", "문", "양", "손", "배", "조", "백", "허", "유"
    };

    private static final String[] FIRST_NAMES = {
            "민준", "서준", "예준", "도윤", "시우", "주원", "하준", "지호", "지후", "준서",
            "서진", "은우", "현우", "연우", "정우", "승우", "시원", "민재", "현준", "원준",
            "지원", "서현", "서윤", "지우", "하은", "민서", "윤서", "수아", "소율", "지안",
            "채원", "예원", "유나", "서아", "다은", "예은", "시은", "하린", "연서", "수빈",
            "영희", "철수", "영수", "순자", "미영", "정호", "승현", "태현", "진우", "상훈"
    };
    @Transactional
    public int generateUsers(int count) {
        log.info("사용자 생성 시작: {}명", count);
        List<User> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setName(generateRandomName());
            user.setMembershipLevel(MembershipLevel.SILVER); // 고정
            user.setCreatedAt(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(30)));

            batch.add(user);

            if (batch.size() >= USER_BATCH_SIZE || i == count - 1) {
                userRepository.saveAll(batch);
                batch.clear();
                log.info("진행률: {}/{}", i + 1, count);
            }
        }

        return count;
    }

    @Transactional
    public int generateOrders(int count) {
        log.info("주문 생성 시작: {}건", count);
        List<Long> userIds = userRepository.findAll().stream().map(User::getId).toList();

        if (userIds.isEmpty()) throw new IllegalStateException("유저가 없습니다.");

        List<Order> batch = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setUser(userRepository.getReferenceById(userIds.get(rand.nextInt(userIds.size()))));
            order.setOrderAmount(generateRandomAmount());
            order.setStatus(getRandomOrderStatus());
            order.setOrderedAt(generateRandomDateInMay());

            batch.add(order);

            if (batch.size() >= ORDER_BATCH_SIZE || i == count - 1) {
                orderRepository.saveAll(batch);
                batch.clear();
                log.info("주문 진행률: {}/{}", i + 1, count);
            }
        }

        return count;
    }

    private String generateRandomName() {
        String last = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
        String first = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
        return last + first;
    }
//1000~500000
    private BigDecimal generateRandomAmount() {
        int amount = ThreadLocalRandom.current().nextInt(10, 5001) * 100;
        return BigDecimal.valueOf(amount);
    }

    private OrderStatus getRandomOrderStatus() {
        int r = ThreadLocalRandom.current().nextInt(100);
        if (r < 85) return OrderStatus.PAID;
        else if (r < 95) return OrderStatus.CANCELLED;
        else return OrderStatus.REFUNDED;
    }

    private LocalDateTime generateRandomDateInMay() {
        int day = ThreadLocalRandom.current().nextInt(1, 32);
        int hour = ThreadLocalRandom.current().nextInt(0, 24);
        int min = ThreadLocalRandom.current().nextInt(0, 60);
        int sec = ThreadLocalRandom.current().nextInt(0, 60);
        return LocalDateTime.of(2025, 5, day, hour, min, sec);
    }


    public void deleteAllData() {
        log.info("모든 데이터 삭제 시작");
        orderRepository.deleteAll();
        userRepository.deleteAll();
        log.info("모든 데이터 삭제 완료");
    }

}
