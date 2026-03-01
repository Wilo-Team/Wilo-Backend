package com.wilo.server.community;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityPost;
import com.wilo.server.community.repository.CommunityCommentRepository;
import com.wilo.server.community.repository.CommunityPostImageRepository;
import com.wilo.server.community.repository.CommunityPostLikeRepository;
import com.wilo.server.community.repository.CommunityPostRepository;
import com.wilo.server.global.config.security.jwt.JwtAuthentication;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private CommunityCommentRepository communityCommentRepository;

    @Autowired
    private CommunityPostLikeRepository communityPostLikeRepository;

    @Autowired
    private CommunityPostImageRepository communityPostImageRepository;

    @BeforeEach
    void setUp() {
        communityCommentRepository.deleteAll();
        communityPostLikeRepository.deleteAll();
        communityPostImageRepository.deleteAll();
        communityPostRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getPosts_returnsCreatedAtAndDaysAgo() throws Exception {
        User user = saveUser("list@example.com", "listUser");
        communityPostRepository.save(
                CommunityPost.builder()
                        .user(user)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("첫 글")
                        .content("커뮤니티 내용")
                        .build()
        );

        mockMvc.perform(get("/api/v1/community/posts")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items[0].title").value("첫 글"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].daysAgo").isNumber())
                .andExpect(jsonPath("$.data.cursor").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].categoryName").value("나무그늘"));
    }

    @Test
    void getPosts_cursorPagination_returnsNextCursor() throws Exception {
        User user = saveUser("cursor@example.com", "cursorUser");
        for (int i = 0; i < 3; i++) {
            communityPostRepository.save(
                    CommunityPost.builder()
                            .user(user)
                            .category(CommunityCategory.SUPPORT_ROOT)
                            .title("커서 테스트 " + i)
                            .content("본문 " + i)
                            .build()
            );
        }

        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/community/posts")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andReturn();
        printPrettyResponse(firstPageResult);

        JsonNode firstPageJson = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("nextCursor").asText();

        mockMvc.perform(get("/api/v1/community/posts")
                        .param("size", "2")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cursor").value(nextCursor))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void comment_and_reply_areNestedInDetail() throws Exception {
        User user = saveUser("comment@example.com", "commentUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(user)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("댓글 테스트")
                        .content("댓글 본문")
                        .build()
        );

        MvcResult parentResult = mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"부모 댓글"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("부모 댓글"))
                .andReturn();
        printPrettyResponse(parentResult);

        JsonNode parentJson = objectMapper.readTree(parentResult.getResponse().getContentAsString());
        long parentCommentId = parentJson.path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCommentId":%d,"content":"답글"}
                                """.formatted(parentCommentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("답글"));

        mockMvc.perform(get("/api/v1/community/posts/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].content").value("부모 댓글"))
                .andExpect(jsonPath("$.data.comments[0].replies[0].content").value("답글"))
                .andExpect(jsonPath("$.data.commentCount").value(2));
    }

    @Test
    void like_and_unlike_updatesCount() throws Exception {
        User user = saveUser("like@example.com", "likeUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(user)
                        .category(CommunityCategory.HELP_BRANCH)
                        .title("좋아요 테스트")
                        .content("좋아요 본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(delete("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    void updatePost_byAuthor_success() throws Exception {
        User author = saveUser("author@example.com", "authorUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("수정 전 제목")
                        .content("수정 전 내용")
                        .build()
        );

        String updateRequest = """
                {
                  "category": "SUNNY_PLACE",
                  "title": "수정 후 제목",
                  "content": "수정 후 내용",
                  "imageUrls": [
                    "https://example.com/1.png",
                    "https://example.com/2.png"
                  ]
                }
                """;

        mockMvc.perform(patch("/api/v1/community/posts/{postId}", post.getId())
                        .with(authentication(new JwtAuthentication(author.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(post.getId()));

        mockMvc.perform(get("/api/v1/community/posts/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정 후 제목"))
                .andExpect(jsonPath("$.data.content").value("수정 후 내용"))
                .andExpect(jsonPath("$.data.category").value("SUNNY_PLACE"))
                .andExpect(jsonPath("$.data.imageUrls.length()").value(2));
    }

    @Test
    void updatePost_byNonAuthor_forbidden() throws Exception {
        User author = saveUser("author2@example.com", "authorUser2");
        User other = saveUser("other@example.com", "otherUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("원본 제목")
                        .content("원본 내용")
                        .build()
        );

        String updateRequest = """
                {
                  "category": "SUNNY_PLACE",
                  "title": "변경 시도",
                  "content": "변경 시도 내용",
                  "imageUrls": []
                }
                """;

        mockMvc.perform(patch("/api/v1/community/posts/{postId}", post.getId())
                        .with(authentication(new JwtAuthentication(other.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(5005));
    }

    @Test
    void deletePost_byAuthor_success() throws Exception {
        User author = saveUser("delete-author@example.com", "deleteAuthor");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("삭제 대상")
                        .content("삭제 내용")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/community/posts/{postId}", post.getId())
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));

        mockMvc.perform(get("/api/v1/community/posts/{postId}", post.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(5001));
    }

    @Test
    void deletePost_byNonAuthor_forbidden() throws Exception {
        User author = saveUser("delete-author2@example.com", "deleteAuthor2");
        User other = saveUser("delete-other@example.com", "deleteOther");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("삭제 권한 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(delete("/api/v1/community/posts/{postId}", post.getId())
                        .with(authentication(new JwtAuthentication(other.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(5006));
    }

    @Test
    void reply_on_reply_isRejected() throws Exception {
        User user = saveUser("depth@example.com", "depthUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(user)
                        .category(CommunityCategory.HELP_BRANCH)
                        .title("깊이 제한 테스트")
                        .content("본문")
                        .build()
        );

        MvcResult parentResult = mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"부모 댓글\"}"))
                .andExpect(status().isOk())
                .andReturn();
        printPrettyResponse(parentResult);
        long parentId = objectMapper.readTree(parentResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult childResult = mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCommentId":%d,"content":"1차 답글"}
                                """.formatted(parentId)))
                .andExpect(status().isOk())
                .andReturn();
        printPrettyResponse(childResult);
        long childId = objectMapper.readTree(childResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCommentId":%d,"content":"2차 답글 시도"}
                                """.formatted(childId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(5004));
    }

    @Test
    void getPostsByAuthor_latestCursorPagination_success() throws Exception {
        User author = saveUser("author-list@example.com", "authorList");
        User other = saveUser("other-list@example.com", "otherList");

        for (int i = 0; i < 3; i++) {
            communityPostRepository.save(
                    CommunityPost.builder()
                            .user(author)
                            .category(CommunityCategory.TREE_SHADE)
                            .title("작성자 글 " + i)
                            .content("작성자 본문 " + i)
                            .build()
            );
        }

        communityPostRepository.save(
                CommunityPost.builder()
                        .user(other)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("다른 사람 글")
                        .content("다른 사람 본문")
                        .build()
        );

        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/community/users/{userId}/posts", author.getId())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].title").value("작성자 글 2"))
                .andReturn();
        printPrettyResponse(firstPageResult);

        JsonNode firstPageJson = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("nextCursor").asText();

        mockMvc.perform(get("/api/v1/community/users/{userId}/posts", author.getId())
                        .param("size", "2")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("작성자 글 0"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void getCommentsByAuthor_latestCursorPagination_success() throws Exception {
        User postAuthor = saveUser("post-owner@example.com", "postOwner");
        User commenter = saveUser("comment-owner@example.com", "commentOwner");
        User other = saveUser("other-commenter@example.com", "otherCommenter");

        CommunityPost post1 = communityPostRepository.save(
                CommunityPost.builder()
                        .user(postAuthor)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("댓글 대상 글 1")
                        .content("본문1")
                        .build()
        );
        CommunityPost post2 = communityPostRepository.save(
                CommunityPost.builder()
                        .user(postAuthor)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("댓글 대상 글 2")
                        .content("본문2")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post1.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"내 댓글 1"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post2.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"내 댓글 2"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post2.getId())
                        .with(authentication(new JwtAuthentication(other.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"다른 사람 댓글"}
                                """))
                .andExpect(status().isOk());

        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/community/users/{userId}/comments", commenter.getId())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].postTitle").value("댓글 대상 글 2"))
                .andExpect(jsonPath("$.data.items[0].content").value("내 댓글 2"))
                .andReturn();
        printPrettyResponse(firstPageResult);

        JsonNode firstPageJson = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("nextCursor").asText();

        mockMvc.perform(get("/api/v1/community/users/{userId}/comments", commenter.getId())
                        .param("size", "1")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].postTitle").value("댓글 대상 글 1"))
                .andExpect(jsonPath("$.data.items[0].content").value("내 댓글 1"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("encodedPassword")
                        .nickname(nickname)
                        .description("desc")
                        .profileImageUrl("https://example.com/profile.png")
                        .build()
        );
    }

    private void printPrettyResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("==== API 응답 결과 ====");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json)));
    }
}
