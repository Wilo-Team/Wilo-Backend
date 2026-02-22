package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityPost;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @Query("""
            SELECT p
            FROM CommunityPost p
            WHERE (:category IS NULL OR p.category = :category)
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE CONCAT('%', LOWER(:keyword), '%')
                   OR LOWER(p.content) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:cursorCreatedAt IS NULL OR p.createdAt < :cursorCreatedAt
                   OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<CommunityPost> findLatestPostsByCursor(
            @Param("category") CommunityCategory category,
            @Param("keyword") String keyword,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM CommunityPost p
            WHERE (:category IS NULL OR p.category = :category)
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE CONCAT('%', LOWER(:keyword), '%')
                   OR LOWER(p.content) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:cursorLikeCount IS NULL
                   OR p.likeCount < :cursorLikeCount
                   OR (p.likeCount = :cursorLikeCount AND p.createdAt < :cursorCreatedAt)
                   OR (p.likeCount = :cursorLikeCount AND p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
            ORDER BY p.likeCount DESC, p.createdAt DESC, p.id DESC
            """)
    List<CommunityPost> findRecommendedPostsByCursor(
            @Param("category") CommunityCategory category,
            @Param("keyword") String keyword,
            @Param("cursorLikeCount") Long cursorLikeCount,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
