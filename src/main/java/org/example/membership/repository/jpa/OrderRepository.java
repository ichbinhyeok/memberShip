package org.example.membership.repository.jpa;


import org.example.membership.common.enums.OrderStatus;
import org.example.membership.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUser_Id(Long userId);


    @Query(value = "SELECT o.user_id, SUM(oi.item_price * oi.quantity - IFNULL(c.discount_amount,0)) " +
            "FROM orders o " +
            "JOIN order_items oi ON oi.order_id = o.id " +
            "LEFT JOIN coupons c ON o.coupon_id = c.id " +
            "WHERE o.status = 'PAID' AND o.ordered_at BETWEEN :start AND :end " +
            "GROUP BY o.user_id", nativeQuery = true)
    List<Object[]> sumOrderAmountByUserBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value =
            "SELECT o.user_id, p.category_id, COUNT(oi.id) AS item_cnt, " +
                    "       SUM(oi.item_price * oi.quantity - IFNULL(c.discount_amount,0)) AS net_amount " +
                    "FROM orders o " +
                    "JOIN order_items oi ON oi.order_id = o.id " +
                    "JOIN products p ON oi.product_id = p.id " +
                    "LEFT JOIN coupons c ON o.coupon_id = c.id " +
                    "WHERE o.status = 'PAID' " +
                    "  AND o.ordered_at >= :start " +
                    "  AND o.ordered_at <  :end " +
                    "  AND o.user_id BETWEEN :startUserId AND :endUserId " +
                    "GROUP BY o.user_id, p.category_id " +
                    "ORDER BY o.user_id, p.category_id",
            nativeQuery = true)
    List<Object[]> aggregateByUserAndCategoryBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("startUserId") long startUserId,
            @Param("endUserId") long endUserId
    );


    @Query(
            value = """
        SELECT
            o.user_id,
            p.category_id,
            COUNT(o.id) AS order_count,
            SUM(oi.item_price * oi.quantity - IFNULL(c.discount_amount, 0)) AS total_amount
        FROM
            orders o
        JOIN
            order_items oi ON o.id = oi.order_id
        JOIN
            products p ON oi.product_id = p.id
        LEFT JOIN
            coupons c ON o.coupon_id = c.id
        WHERE
            o.status = 'PAID'
            AND o.ordered_at >= :start
            AND o.ordered_at < :end
        GROUP BY
            o.user_id,
            p.category_id
    """,
            nativeQuery = true
    )
// 반환 타입이 Object 배열인 것은 동일
    List<Object[]> aggregateUserCategoryStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}