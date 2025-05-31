package org.example.membership.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.membership.common.enums.MembershipLevel;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByMembershipLevel(MembershipLevel level);
} 