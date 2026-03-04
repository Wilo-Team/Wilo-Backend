package com.wilo.server.community.repository;

import com.wilo.server.community.entity.post.CommunityPost;
import com.wilo.server.community.repository.query.CommunityPostQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, CommunityPostQueryRepository {
}
