package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityPostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage, Long> {

    List<CommunityPostImage> findByPostIdOrderBySortOrderAsc(Long postId);
}
