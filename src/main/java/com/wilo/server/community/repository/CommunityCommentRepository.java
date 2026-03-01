package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    List<CommunityComment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId);
}
