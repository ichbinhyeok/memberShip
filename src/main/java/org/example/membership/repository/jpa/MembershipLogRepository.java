package org.example.membership.repository.jpa;

import org.example.membership.entity.MembershipLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipLogRepository extends JpaRepository<MembershipLog, Long> {
}