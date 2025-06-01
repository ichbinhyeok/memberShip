package org.example.membership.domain.log.jpa;

import org.example.membership.domain.log.MembershipLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipLogRepository extends JpaRepository<MembershipLog, Long> {
}