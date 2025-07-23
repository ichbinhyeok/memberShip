package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;
import java.util.UUID;
import com.github.f4b6a3.uuid.UuidCreator;
import org.example.membership.common.util.ShardPrefixedUuidGenerator;
import org.example.membership.common.util.ShardUuidGenerator;
@Entity
@Table(name = "coupon_issue_log")
@Getter
@Setter
@NoArgsConstructor
public class CouponIssueLog {

    @Id
    @Column(length = 40) // 샤드(2) + '_' + UUID(36) = 최대 39~40자
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false)
    private MembershipLevel membershipLevel;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            int shardNo = resolveShardNoFromUser(this.user);
            this.id = ShardPrefixedUuidGenerator.generate(shardNo);
        }
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }

    private int resolveShardNoFromUser(User user) {
        return Math.abs(user.getId().hashCode() % 100); // 00~99 샤드
    }
}
