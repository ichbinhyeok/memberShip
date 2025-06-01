package org.example.membership.domain.user.jpa;

import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByMembershipLevel(MembershipLevel level);
} 