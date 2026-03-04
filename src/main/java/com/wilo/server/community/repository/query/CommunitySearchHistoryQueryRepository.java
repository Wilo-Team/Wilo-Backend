package com.wilo.server.community.repository.query;

import com.wilo.server.community.entity.search.CommunitySearchHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CommunitySearchHistoryQueryRepository {

    List<CommunitySearchHistory> findLatestByUserIdCursor(
            Long userId,
            LocalDateTime cursorLastSearchedAt,
            Long cursorId,
            Pageable pageable
    );
}
