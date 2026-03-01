package com.wilo.server.community.repository.query;

import com.wilo.server.community.entity.CommunityPostLike;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CommunityPostLikeQueryRepository {

    List<CommunityPostLike> findLikedPostsByUserIdCursor(
            Long userId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );
}
