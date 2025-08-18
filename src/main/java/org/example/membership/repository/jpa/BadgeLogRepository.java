package org.example.membership.repository.jpa;

import org.example.membership.entity.BadgeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BadgeLogRepository extends JpaRepository<BadgeLog,UUID> {
}
