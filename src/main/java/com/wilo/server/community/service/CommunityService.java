package com.wilo.server.community.service;

import com.wilo.server.community.dto.CommunityCommentCreateRequestDto;
import com.wilo.server.community.dto.CommunityCommentDto;
import com.wilo.server.community.dto.CommunityLikeResponseDto;
import com.wilo.server.community.dto.CommunityPostAuthorDto;
import com.wilo.server.community.dto.CommunityPostCreateRequestDto;
import com.wilo.server.community.dto.CommunityPostDetailResponseDto;
import com.wilo.server.community.dto.CommunityPostListResponseDto;
import com.wilo.server.community.dto.CommunityPostSummaryDto;
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
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @Transactional(readOnly = true)
    public CommunityPostListResponseDto getPosts(
            CommunityCategory category,
            CommunityPostSortType sort,
            String keyword,
            Integer page,
            Integer size
    ) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        CommunityPostSortType sortType = sort == null ? CommunityPostSortType.LATEST : sort;

        Pageable pageable = PageRequest.of(safePage, safeSize, sortType.getSort());
        Specification<CommunityPost> specification = createPostSpecification(category, keyword);

        Page<CommunityPost> resultPage = communityPostRepository.findAll(specification, pageable);

        List<CommunityPostSummaryDto> items = resultPage.getContent().stream()
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

        return new CommunityPostListResponseDto(items, safePage, safeSize, resultPage.hasNext());
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

    private Specification<CommunityPost> createPostSpecification(CommunityCategory category, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likeKeyword),
                        cb.like(cb.lower(root.get("content")), likeKeyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
}
