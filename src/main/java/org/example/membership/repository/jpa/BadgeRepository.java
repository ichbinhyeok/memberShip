package org.example.membership.repository.jpa;


import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    boolean existsByUserAndCategory(User user, Category category);
    long countByUser(User user);
    java.util.List<Badge> findByUser(User user);
    long countByUserAndActiveTrue(User user);
    java.util.List<Badge> findByUserAndActiveTrue(User user);
    @Query(value = "SELECT user_id, COUNT(*) AS active_count " +
            "FROM badges " +
            "WHERE active = true " +
            "GROUP BY user_id", nativeQuery = true)
    List<Object[]> countActiveBadgesGroupedByUserId();

    List<Badge> findAllByUserInAndActiveTrue(List<User> users);

    List<Badge> findAllByUserIn(List<User> users);

    java.util.Optional<Badge> findByUserAndCategory(User user, Category category);

    Optional<Badge> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Badge> findAllByUserIdIn(Collection<Long> userIds);

}

