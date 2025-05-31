package org.example.membership.domain.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipLogRepository extends JpaRepository<MembershipLog, Long> {
    List<MembershipLog> findByUserIdOrderByChangedAtDesc(Long userId);
} 