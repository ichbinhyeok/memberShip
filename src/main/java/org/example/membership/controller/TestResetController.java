package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-utils")
@RequiredArgsConstructor
public class TestResetController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/reset")
    public ResponseEntity<String> resetTestData() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        jdbcTemplate.execute("TRUNCATE TABLE coupon_usage");
        jdbcTemplate.execute("TRUNCATE TABLE coupon_issue_log");
        jdbcTemplate.execute("TRUNCATE TABLE membership_log");
        jdbcTemplate.execute("TRUNCATE TABLE badges");
        jdbcTemplate.execute("TRUNCATE TABLE order_items");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE users");

        jdbcTemplate.execute("INSERT INTO users SELECT * FROM users_original");
        jdbcTemplate.execute("INSERT INTO orders SELECT * FROM orders_original");
        jdbcTemplate.execute("INSERT INTO order_items SELECT * FROM order_item_original");
        jdbcTemplate.execute("INSERT INTO badges SELECT * FROM badge_original");
        jdbcTemplate.execute("INSERT INTO membership_log SELECT * FROM membership_log_original");
        jdbcTemplate.execute("INSERT INTO coupon_issue_log SELECT * FROM coupon_issue_log_original");
        jdbcTemplate.execute("INSERT INTO coupon_usage SELECT * FROM coupon_usage_original");

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        return ResponseEntity.ok("데이터 초기화 완료");
    }
}
