package org.example.membership.repository.jpa;

import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByMembershipLevel(MembershipLevel level);

    Optional<User> findByName(String name);
}