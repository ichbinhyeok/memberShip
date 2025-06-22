package org.example.membership.repository.jpa;


import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    boolean existsByUserAndCategory(User user, Category category);
    long countByUser(User user);
    java.util.List<Badge> findByUser(User user);
}