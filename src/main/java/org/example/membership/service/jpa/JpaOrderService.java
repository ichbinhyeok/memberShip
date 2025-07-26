package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.config.MyWasInstanceHolder;
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

    private final MyWasInstanceHolder myWasInstanceHolder;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {

        // [WAS Sharding Logic]
        if (!myWasInstanceHolder.isMyUser(request.getUserId())) {
            return null;
        }

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

        // [WAS Sharding Logic]
        if (!myWasInstanceHolder.isMyUser(userId)) {
            return List.of();
        }
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

    @Transactional(readOnly = true)
    public Map<Long, Map<Long, OrderCountAndAmount>> aggregateUserCategoryStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(startDateTime, endDateTime);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (Object[] row : aggregates) {
            Long userId = (Long) row[0];
            Long categoryId = (Long) row[1];
            Long count = ((Number) row[2]).longValue();
            BigDecimal amount = (BigDecimal) row[3];

            Map<Long, OrderCountAndAmount> categoryMap = statMap.computeIfAbsent(userId, k -> new HashMap<>());
            categoryMap.put(categoryId, new OrderCountAndAmount(count, amount));
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