package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;
import java.util.UUID;
import com.github.f4b6a3.uuid.UuidCreator;

@Entity
@Table(name = "coupon_issue_log")
@Getter
@Setter
@NoArgsConstructor
public class CouponIssueLog {
    @Id
    @Column(length = 36)
    private UUID id;

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
            this.id = UuidCreator.getTimeOrdered();
        }
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }

}