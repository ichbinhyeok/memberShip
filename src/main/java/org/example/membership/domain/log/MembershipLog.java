package org.example.membership.domain.log;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.domain.user.User;
import org.example.membership.domain.user.MembershipLevel;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_log")
@Getter
@Setter
@NoArgsConstructor
public class MembershipLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_level")
    private MembershipLevel previousLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_level")
    private MembershipLevel newLevel;

    @Column(name = "change_reason", length = 200)
    private String changeReason;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
} 