package org.example.membership.repository.jpa;

import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByMembershipLevel(MembershipLevel level);

    Optional<User> findByName(String name);

    @Query("SELECT MIN(u.id) FROM User u")
    long findMinUserId();

    @Query("SELECT MAX(u.id) FROM User u")
    long findMaxUserId();

    @Query("""
    SELECT u FROM User u
    WHERE u.id BETWEEN :startId AND :endId
    ORDER BY u.id ASC
""")
    List<User> findUsersInRange(@Param("startId") long startId, @Param("endId") long endId);

}