package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.*;
import org.example.membership.entity.*;
import org.example.membership.exception.ConflictException;
import org.example.membership.exception.NotFoundException;
import org.example.membership.repository.jpa.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaOrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;
    private final CouponUsageRepository couponUsageRepository;


    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {


        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setOrderedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setItemPrice(product.getPrice());
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        CouponUsage usage = null;

        if (request.getCouponIssueId() != null) {
            String issueId = request.getCouponIssueId();
            if (couponUsageRepository.existsByCouponIssueLog_Id(issueId)) {
                throw new ConflictException("Coupon already used");
            }

            CouponIssueLog log = couponIssueLogRepository.findById(issueId)
                    .orElseThrow(() -> new NotFoundException("Coupon issue not found"));

            usage = new CouponUsage();
            usage.setUser(user);
            usage.setCoupon(log.getCoupon());
            usage.setOrder(order);
            usage.setCouponIssueLog(log);
            couponUsageRepository.save(usage);
        }

        return toOrderResponse(order, usage);
    }


    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {

        List<Order> orders = orderRepository.findByUser_Id(userId);

        return orders.stream().map(order -> {
            OrderResponse dto = new OrderResponse();
            dto.setId(order.getId());
            dto.setUserId(order.getUser().getId());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setStatus(order.getStatus());
            dto.setOrderedAt(order.getOrderedAt());
            return dto;
        }).toList();
    }

    /**
     * 지정된 날짜(targetDate) 기준 최근 3개월 동안,
     * 컷오프 시점(cutoffAt) 이전에 발생한 주문 통계를 집계합니다.
     *
     * @param targetDate 통계의 기준이 되는 날짜 (예: 2025-06-01)
     * @param cutoffAt   데이터 집계의 상한선이 되는 정확한 시간 (T0)
     * @param startUserId 현재 was에 필요한 범위만큼만
     * @param endUserId
     * @return 사용자별, 카테고리별 주문 횟수 및 금액 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, OrderCountAndAmount>> aggregateUserCategoryStats(
            LocalDate targetDate,
            LocalDateTime cutoffAt,
            long startUserId,
            long endUserId
    ) {
        LocalDateTime startDateTime = targetDate.withDayOfMonth(1).minusMonths(3).atStartOfDay();
        List<Object[]> rows = orderRepository.aggregateByUserAndCategoryBetween(startDateTime, cutoffAt, startUserId, endUserId);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();
        for (Object[] r : rows) {
            Long userId = (Long) r[0];
            Long categoryId = (Long) r[1];
            Long cnt = ((Number) r[2]).longValue();
            BigDecimal amt = (BigDecimal) r[3];
            statMap.computeIfAbsent(userId, k -> new HashMap<>())
                    .put(categoryId, new OrderCountAndAmount(cnt, amt));
        }
        return statMap;
    }



    private OrderResponse toOrderResponse(Order order, CouponUsage usage) {
        BigDecimal original = order.getTotalAmount();
        BigDecimal discount = usage != null
                ? usage.getCoupon().getDiscountAmount().getAmount()
                : BigDecimal.ZERO;
        BigDecimal finalAmount = original.subtract(discount);

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setStatus(order.getStatus());
        response.setOrderedAt(order.getOrderedAt());
        response.setOriginalAmount(original);
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> {
                    OrderItemResponse r = new OrderItemResponse();
                    r.setProductId(item.getProduct().getId());
                    r.setProductName(item.getProduct().getName());
                    r.setItemPrice(item.getItemPrice());
                    r.setQuantity(item.getQuantity());
                    return r;
                }).toList();

        response.setItems(items);
        return response;
    }


}