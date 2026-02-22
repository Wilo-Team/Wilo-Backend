package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>, JpaSpecificationExecutor<CommunityPost> {
}
