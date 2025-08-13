package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.batch.LevelResult;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipLevel membershipLevel = MembershipLevel.NONE;

    @Column(name = "last_membership_change")
    private LocalDateTime lastMembershipChange;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public User(String name) {
        this.name = name;
    }

    public void applyLevelFromResult(LevelResult result) {
        this.membershipLevel = result.getNewLevel();
        this.lastMembershipChange = LocalDateTime.now();
    }

} 