package com.wilo.server.community.service.search;

import com.wilo.server.community.dto.post.CommunityPostListResponseDto;
import com.wilo.server.community.dto.post.CommunityPostSummaryDto;
import com.wilo.server.community.dto.search.CommunitySearchHistoryItemDto;
import com.wilo.server.community.dto.search.CommunitySearchHistoryListResponseDto;
import com.wilo.server.community.entity.post.CommunityCategory;
import com.wilo.server.community.entity.post.CommunityPost;
import com.wilo.server.community.entity.post.CommunityPostSortType;
import com.wilo.server.community.entity.search.CommunitySearchHistory;
import com.wilo.server.community.error.CommunityErrorCase;
import com.wilo.server.community.repository.CommunityPostRepository;
import com.wilo.server.community.repository.CommunitySearchHistoryRepository;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunitySearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int CONTENT_PREVIEW_LENGTH = 120;

    private final CommunityPostRepository communityPostRepository;
    private final CommunitySearchHistoryRepository communitySearchHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommunityPostListResponseDto getPosts(
            Long requesterUserId,
            CommunityCategory category,
            CommunityPostSortType sort,
            String keyword,
            String cursor,
            Integer size
    ) {
        saveSearchKeywordIfNeeded(requesterUserId, keyword);

        int safeSize = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        CommunityPostSortType sortType = sort == null ? CommunityPostSortType.RECOMMENDED : sort;
        Pageable pageable = PageRequest.of(0, safeSize + 1);

        List<CommunityPost> fetchedPosts = switch (sortType) {
            case LATEST -> {
                LatestCursor latestCursor = LatestCursor.from(cursor);
                yield communityPostRepository.findLatestPostsByCursor(
                        category,
                        keyword,
                        latestCursor.createdAt(),
                        latestCursor.id(),
                        pageable
                );
            }
            case RECOMMENDED -> {
                RecommendedCursor recommendedCursor = RecommendedCursor.from(cursor);
                yield communityPostRepository.findRecommendedPostsByCursor(
                        category,
                        keyword,
                        recommendedCursor.likeCount(),
                        recommendedCursor.createdAt(),
                        recommendedCursor.id(),
                        pageable
                );
            }
        };

        boolean hasNext = fetchedPosts.size() > safeSize;
        List<CommunityPost> pagePosts = hasNext ? fetchedPosts.subList(0, safeSize) : fetchedPosts;

        List<CommunityPostSummaryDto> items = pagePosts.stream()
                .map(post -> new CommunityPostSummaryDto(
                        post.getId(),
                        post.getCategory(),
                        post.getCategory().getDisplayName(),
                        post.getTitle(),
                        createPreview(post.getContent()),
                        post.getCreatedAt(),
                        calculateDaysAgo(post.getCreatedAt()),
                        post.getViewCount(),
                        post.getLikeCount(),
                        post.getCommentCount()
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !pagePosts.isEmpty()) {
            CommunityPost lastPost = pagePosts.get(pagePosts.size() - 1);
            nextCursor = switch (sortType) {
                case LATEST -> LatestCursor.of(lastPost).toCursorValue();
                case RECOMMENDED -> RecommendedCursor.of(lastPost).toCursorValue();
            };
        }

        return new CommunityPostListResponseDto(items, cursor, safeSize, hasNext, nextCursor);
    }

    @Transactional(readOnly = true)
    public CommunitySearchHistoryListResponseDto getSearchHistories(
            Long userId,
            String cursor,
            Integer size
    ) {
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, safeSize + 1);

        LatestCursor latestCursor = LatestCursor.from(cursor);
        List<CommunitySearchHistory> fetchedHistories = communitySearchHistoryRepository.findLatestByUserIdCursor(
                userId,
                latestCursor.createdAt(),
                latestCursor.id(),
                pageable
        );

        boolean hasNext = fetchedHistories.size() > safeSize;
        List<CommunitySearchHistory> pageHistories = hasNext
                ? fetchedHistories.subList(0, safeSize)
                : fetchedHistories;

        List<CommunitySearchHistoryItemDto> items = pageHistories.stream()
                .map(history -> new CommunitySearchHistoryItemDto(
                        history.getId(),
                        history.getKeyword(),
                        history.getLastSearchedAt()
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !pageHistories.isEmpty()) {
            CommunitySearchHistory lastHistory = pageHistories.get(pageHistories.size() - 1);
            nextCursor = lastHistory.getLastSearchedAt() + "|" + lastHistory.getId();
        }

        return new CommunitySearchHistoryListResponseDto(items, cursor, safeSize, hasNext, nextCursor);
    }

    @Transactional
    public void deleteSearchHistory(Long userId, Long historyId) {
        CommunitySearchHistory history = communitySearchHistoryRepository.findById(historyId)
                .orElseThrow(() -> ApplicationException.from(CommunityErrorCase.SEARCH_HISTORY_NOT_FOUND));

        if (!history.getUser().getId().equals(userId)) {
            throw ApplicationException.from(CommunityErrorCase.FORBIDDEN_SEARCH_HISTORY_ACCESS);
        }

        communitySearchHistoryRepository.delete(history);
    }

    @Transactional
    public int deleteAllSearchHistories(Long userId) {
        return communitySearchHistoryRepository.deleteByUserId(userId);
    }

    private void saveSearchKeywordIfNeeded(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank()) {
            return;
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        communitySearchHistoryRepository.findByUserIdAndKeyword(userId, normalizedKeyword)
                .ifPresentOrElse(
                        history -> {
                            history.touch(now);
                            communitySearchHistoryRepository.save(history);
                        },
                        () -> communitySearchHistoryRepository.save(
                                CommunitySearchHistory.builder()
                                        .user(userRepository.getReferenceById(userId))
                                        .keyword(normalizedKeyword)
                                        .lastSearchedAt(now)
                                        .build()
                        )
                );
    }

    private long calculateDaysAgo(LocalDateTime createdAt) {
        return ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }

    private String createPreview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
    }

    private record LatestCursor(LocalDateTime createdAt, Long id) {
        private static LatestCursor from(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return new LatestCursor(null, null);
            }

            String[] parts = cursor.split("\\|");
            if (parts.length != 2) {
                return new LatestCursor(null, null);
            }

            try {
                return new LatestCursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
            } catch (RuntimeException e) {
                return new LatestCursor(null, null);
            }
        }

        private static LatestCursor of(CommunityPost post) {
            return new LatestCursor(post.getCreatedAt(), post.getId());
        }

        private String toCursorValue() {
            return createdAt + "|" + id;
        }
    }

    private record RecommendedCursor(Long likeCount, LocalDateTime createdAt, Long id) {
        private static RecommendedCursor from(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return new RecommendedCursor(null, null, null);
            }

            String[] parts = cursor.split("\\|");
            if (parts.length != 3) {
                return new RecommendedCursor(null, null, null);
            }

            try {
                return new RecommendedCursor(
                        Long.parseLong(parts[0]),
                        LocalDateTime.parse(parts[1]),
                        Long.parseLong(parts[2])
                );
            } catch (RuntimeException e) {
                return new RecommendedCursor(null, null, null);
            }
        }

        private static RecommendedCursor of(CommunityPost post) {
            return new RecommendedCursor(post.getLikeCount(), post.getCreatedAt(), post.getId());
        }

        private String toCursorValue() {
            return likeCount + "|" + createdAt + "|" + id;
        }
    }
}
