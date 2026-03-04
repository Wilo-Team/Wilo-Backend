package com.wilo.server.community.repository.query;

import com.wilo.server.community.entity.comment.CommunityComment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CommunityCommentQueryRepository {

    List<CommunityComment> findLatestByUserIdCursor(
            Long userId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );
}
