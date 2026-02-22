package com.wilo.server.community.repository.query;

import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityPost;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CommunityPostQueryRepository {

    List<CommunityPost> findLatestPostsByCursor(
            CommunityCategory category,
            String keyword,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );

    List<CommunityPost> findRecommendedPostsByCursor(
            CommunityCategory category,
            String keyword,
            Long cursorLikeCount,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );
}
