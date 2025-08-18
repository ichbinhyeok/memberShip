package org.example.membership.repository.jpa;


import org.example.membership.entity.Badge;
import org.example.membership.entity.Category;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
    SELECT b.user.id, COUNT(b)
    FROM Badge b
    WHERE b.active = true AND b.user.id IN :userIds
    GROUP BY b.user.id
    """)
    List<Object[]> countActiveBadgesGroupedByUserIds(@Param("userIds") List<Long> userIds);

    @Query("""
    SELECT CONCAT(b.user.id, ':', b.category.id)
    FROM Badge b
    WHERE b.user.id BETWEEN :start AND :end
    """)
    List<String> findKeysByUserIdRange(@Param("start") Long start, @Param("end") Long end);


    Optional<Badge> findByUserId(Long userId);

    List<Badge> findByUserIdIn(List<Long> userIds);

    /**
     *  T0 시점의 활성 배지 개수를 재구성하여 계산하는 쿼리
     * @param userIds 대상 사용자 ID 목록
     * @param batchStartTime 배치 시작 시간 (T0)
     * @return [userId, activeBadgeCount] 형태의 Object 배열 리스트
     */
    @Query(value = """
        SELECT
            b.user_id,
            COUNT(b.id)
        FROM
            badges b
        LEFT JOIN
            badge_log bl ON b.id = bl.badge_id AND bl.changed_at >= :batchStartTime
        WHERE
            b.user_id IN :userIds
            AND (CASE
                    WHEN bl.id IS NOT NULL THEN bl.previous_active_status
                    ELSE b.active
                END) = TRUE
        GROUP BY
            b.user_id
    """, nativeQuery = true)
    List<Object[]> countActiveBadgesAsOfT0(@Param("userIds") List<Long> userIds,
                                           @Param("batchStartTime") LocalDateTime batchStartTime);
    @Modifying
    @Query("UPDATE Badge b SET b.active = :newState, b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.user.id = :userId AND b.category.id = :categoryId " +
            "AND b.updatedAt < :batchStartTime")
    int updateBadgeStateConditionally(@Param("userId") Long userId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("newState") boolean newState,
                                      @Param("batchStartTime") LocalDateTime batchStartTime);
}



