package com.wilo.server.community;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/community/posts")
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

        MvcResult firstPageResult = mockMvc.perform(get("/api/community/posts")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andReturn();

        JsonNode firstPageJson = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("nextCursor").asText();

        mockMvc.perform(get("/api/community/posts")
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

        MvcResult parentResult = mockMvc.perform(post("/api/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"부모 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("부모 댓글"))
                .andReturn();

        JsonNode parentJson = objectMapper.readTree(parentResult.getResponse().getContentAsString());
        long parentCommentId = parentJson.path("data").path("id").asLong();

        mockMvc.perform(post("/api/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentCommentId\":" + parentCommentId + ",\"content\":\"답글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("답글"));

        mockMvc.perform(get("/api/community/posts/{postId}", post.getId()))
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

        mockMvc.perform(post("/api/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(delete("/api/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
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
}
