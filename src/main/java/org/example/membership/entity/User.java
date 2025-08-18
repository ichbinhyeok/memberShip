package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.batch.LevelResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // User가 변경될 때 MembershipLog도 함께 저장되도록 Cascade 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipLog> membershipLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public User(String name) {
        this.name = name;
    }

    /**
     * 등급을 변경하고, 변경이 발생했다면 로그를 내부 리스트에 추가
     */
    public void applyLevelAndLog(LevelResult result, String reason) {
        // 등급 변경이 없으면 아무것도 하지 않음
        if (this.membershipLevel == result.getNewLevel()) {
            return;
        }

        // 로그 생성 및 내부 리스트에 추가
        MembershipLog log = new MembershipLog(this, this.membershipLevel, result.getNewLevel(), reason);
        this.membershipLogs.add(log);

        // 등급 및 최종 변경 시간 업데이트
        this.membershipLevel = result.getNewLevel();
        this.lastMembershipChange = LocalDateTime.now();
    }


}