package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityPostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<CommunityPostLike> findByPostIdAndUserId(Long postId, Long userId);

    List<CommunityPostLike> findByPostId(Long postId);
}
