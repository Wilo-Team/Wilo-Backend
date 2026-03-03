package com.wilo.server.community.repository;

import com.wilo.server.community.entity.CommunityComment;
import com.wilo.server.community.repository.query.CommunityCommentQueryRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long>, CommunityCommentQueryRepository {

    List<CommunityComment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId);
}
