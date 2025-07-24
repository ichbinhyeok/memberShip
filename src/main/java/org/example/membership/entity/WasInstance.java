package org.example.membership.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "was_instance")
@Getter
@Setter
@NoArgsConstructor
public class WasInstance {

    @Id
    private UUID id;

    @Column(name = "index_number", nullable = false)
    private int index;

    @Enumerated(EnumType.STRING)
    private Status status = Status.RUNNING;

    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    public enum Status {
        RUNNING, TERMINATED
    }

    public void updateHeartbeat() {
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public boolean isAlive(LocalDateTime threshold) {
        return this.lastHeartbeatAt != null && this.lastHeartbeatAt.isAfter(threshold);
    }
}
