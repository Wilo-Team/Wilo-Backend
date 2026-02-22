package com.wilo.server.community.service;

import com.wilo.server.community.dto.CommunityCommentCreateRequestDto;
import com.wilo.server.community.dto.CommunityCommentDto;
import com.wilo.server.community.dto.CommunityLikeResponseDto;
import com.wilo.server.community.dto.CommunityPostAuthorDto;
import com.wilo.server.community.dto.CommunityPostCreateRequestDto;
import com.wilo.server.community.dto.CommunityPostDetailResponseDto;
import com.wilo.server.community.dto.CommunityPostListResponseDto;
import com.wilo.server.community.dto.CommunityPostSummaryDto;
import com.wilo.server.community.dto.CommunityPostUpdateRequestDto;
import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityComment;
import com.wilo.server.community.entity.CommunityPost;
import com.wilo.server.community.entity.CommunityPostImage;
import com.wilo.server.community.entity.CommunityPostLike;
import com.wilo.server.community.entity.CommunityPostSortType;
import com.wilo.server.community.error.CommunityErrorCase;
import com.wilo.server.community.repository.CommunityCommentRepository;
import com.wilo.server.community.repository.CommunityPostImageRepository;
import com.wilo.server.community.repository.CommunityPostLikeRepository;
import com.wilo.server.community.repository.CommunityPostRepository;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.error.UserErrorCase;
import com.wilo.server.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int CONTENT_PREVIEW_LENGTH = 120;

    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostImageRepository communityPostImageRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createPost(Long userId, CommunityPostCreateRequestDto request) {
        User user = getUserOrThrow(userId);

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(user)
                        .category(request.category())
                        .title(request.title())
                        .content(request.content())
                        .build()
        );

        List<String> imageUrls = request.imageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<CommunityPostImage> images = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                images.add(CommunityPostImage.builder()
                        .post(post)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(i)
                        .build());
            }
            communityPostImageRepository.saveAll(images);
        }

        return post.getId();
    }

    @Transactional
    public Long updatePost(Long userId, Long postId, CommunityPostUpdateRequestDto request) {
        getUserOrThrow(userId);
        CommunityPost post = getPostOrThrow(postId);

        if (!post.getUser().getId().equals(userId)) {
            throw ApplicationException.from(CommunityErrorCase.FORBIDDEN_POST_UPDATE);
        }

        post.updatePost(request.category(), request.title(), request.content());

        List<CommunityPostImage> existingImages = communityPostImageRepository.findByPostIdOrderBySortOrderAsc(postId);
        if (!existingImages.isEmpty()) {
            communityPostImageRepository.deleteAll(existingImages);
        }

        List<String> imageUrls = request.imageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<CommunityPostImage> images = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                images.add(CommunityPostImage.builder()
                        .post(post)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(i)
                        .build());
            }
            communityPostImageRepository.saveAll(images);
        }

        return post.getId();
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        getUserOrThrow(userId);
        CommunityPost post = getPostOrThrow(postId);

        if (!post.getUser().getId().equals(userId)) {
            throw ApplicationException.from(CommunityErrorCase.FORBIDDEN_POST_DELETE);
        }

        List<CommunityComment> comments = communityCommentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId);
        if (!comments.isEmpty()) {
            communityCommentRepository.deleteAll(comments);
        }

        List<CommunityPostImage> images = communityPostImageRepository.findByPostIdOrderBySortOrderAsc(postId);
        if (!images.isEmpty()) {
            communityPostImageRepository.deleteAll(images);
        }

        List<CommunityPostLike> likes = communityPostLikeRepository.findByPostId(postId);
        if (!likes.isEmpty()) {
            communityPostLikeRepository.deleteAll(likes);
        }

        communityPostRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public CommunityPostListResponseDto getPosts(
            CommunityCategory category,
            CommunityPostSortType sort,
            String keyword,
            String cursor,
            Integer size
    ) {
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

    @Transactional
    public CommunityPostDetailResponseDto getPostDetail(Long postId) {
        CommunityPost post = getPostOrThrow(postId);
        post.increaseViewCount();

        List<String> imageUrls = communityPostImageRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
                .map(CommunityPostImage::getImageUrl)
                .toList();

        List<CommunityComment> comments = communityCommentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId);
        List<CommunityCommentDto> commentTree = buildCommentTree(comments);

        return new CommunityPostDetailResponseDto(
                post.getId(),
                post.getCategory(),
                post.getCategory().getDisplayName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                calculateDaysAgo(post.getCreatedAt()),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                CommunityPostAuthorDto.from(post.getUser()),
                imageUrls,
                commentTree
        );
    }

    @Transactional
    public CommunityCommentDto createComment(Long userId, Long postId, CommunityCommentCreateRequestDto request) {
        User user = getUserOrThrow(userId);
        CommunityPost post = getPostOrThrow(postId);

        CommunityComment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = communityCommentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> ApplicationException.from(CommunityErrorCase.COMMENT_NOT_FOUND));

            if (!parentComment.getPost().getId().equals(postId)) {
                throw ApplicationException.from(CommunityErrorCase.INVALID_PARENT_COMMENT);
            }

            if (parentComment.getParentComment() != null) {
                throw ApplicationException.from(CommunityErrorCase.REPLY_DEPTH_NOT_ALLOWED);
            }
        }

        CommunityComment comment = communityCommentRepository.save(
                CommunityComment.builder()
                        .post(post)
                        .user(user)
                        .parentComment(parentComment)
                        .content(request.content())
                        .build()
        );

        post.increaseCommentCount();

        return CommunityCommentDto.of(
                comment.getId(),
                CommunityPostAuthorDto.from(user),
                comment.getContent(),
                calculateDaysAgo(comment.getCreatedAt()),
                comment.getCreatedAt()
        );
    }

    @Transactional
    public CommunityLikeResponseDto likePost(Long userId, Long postId) {
        User user = getUserOrThrow(userId);
        CommunityPost post = getPostOrThrow(postId);

        boolean alreadyLiked = communityPostLikeRepository.existsByPostIdAndUserId(postId, userId);
        if (!alreadyLiked) {
            communityPostLikeRepository.save(
                    CommunityPostLike.builder()
                            .post(post)
                            .user(user)
                            .build()
            );
            post.increaseLikeCount();
        }

        return new CommunityLikeResponseDto(true, post.getLikeCount());
    }

    @Transactional
    public CommunityLikeResponseDto unlikePost(Long userId, Long postId) {
        getUserOrThrow(userId);
        CommunityPost post = getPostOrThrow(postId);

        communityPostLikeRepository.findByPostIdAndUserId(postId, userId)
                .ifPresent(like -> {
                    communityPostLikeRepository.delete(like);
                    post.decreaseLikeCount();
                });

        return new CommunityLikeResponseDto(false, post.getLikeCount());
    }

    private List<CommunityCommentDto> buildCommentTree(List<CommunityComment> comments) {
        Map<Long, CommunityCommentDto> commentMap = new HashMap<>();
        List<CommunityCommentDto> roots = new ArrayList<>();

        for (CommunityComment comment : comments) {
            CommunityCommentDto dto = CommunityCommentDto.of(
                    comment.getId(),
                    CommunityPostAuthorDto.from(comment.getUser()),
                    comment.getContent(),
                    calculateDaysAgo(comment.getCreatedAt()),
                    comment.getCreatedAt()
            );
            commentMap.put(comment.getId(), dto);
        }

        for (CommunityComment comment : comments) {
            CommunityCommentDto dto = commentMap.get(comment.getId());
            CommunityComment parent = comment.getParentComment();

            if (parent == null) {
                roots.add(dto);
                continue;
            }

            CommunityCommentDto parentDto = commentMap.get(parent.getId());
            if (parentDto == null) {
                roots.add(dto);
                continue;
            }

            parentDto.replies().add(dto);
        }

        return roots;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApplicationException.from(UserErrorCase.USER_NOT_FOUND));
    }

    private CommunityPost getPostOrThrow(Long postId) {
        return communityPostRepository.findById(postId)
                .orElseThrow(() -> ApplicationException.from(CommunityErrorCase.POST_NOT_FOUND));
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
