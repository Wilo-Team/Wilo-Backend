package com.wilo.server.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.community.entity.post.CommunityCategory;
import com.wilo.server.community.entity.post.CommunityPost;
import com.wilo.server.community.repository.CommunityCommentRepository;
import com.wilo.server.community.repository.CommunityPostImageRepository;
import com.wilo.server.community.repository.CommunityPostLikeRepository;
import com.wilo.server.community.repository.CommunityPostRepository;
import com.wilo.server.global.config.security.jwt.JwtAuthentication;
import com.wilo.server.notification.repository.UserNotificationRepository;
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
class NotificationControllerTest {

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

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @BeforeEach
    void setUp() {
        communityCommentRepository.deleteAll();
        communityPostLikeRepository.deleteAll();
        communityPostImageRepository.deleteAll();
        userNotificationRepository.deleteAll();
        communityPostRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getNotifications_latestAndCursorPagination_success() throws Exception {
        User author = saveUser("author@example.com", "author");
        User commenter = saveUser("commenter@example.com", "commenter");
        User liker = saveUser("liker@example.com", "liker");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("알림 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"댓글 알림 내용입니다."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(liker.getId()))))
                .andExpect(status().isOk());

        MvcResult firstPage = mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId())))
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("POST_LIKE"))
                .andExpect(jsonPath("$.data.items[0].postId").value(post.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andReturn();
        printPrettyResponse(firstPage);

        JsonNode firstPageJson = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("nextCursor").asText();

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId())))
                        .param("size", "1")
                        .param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("COMMENT"))
                .andExpect(jsonPath("$.data.items[0].commentPreview").value("댓글 알림 내용입니다."))
                .andExpect(jsonPath("$.data.items[0].postId").value(post.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void markAsRead_and_markAllAsRead_success() throws Exception {
        User author = saveUser("author2@example.com", "author2");
        User commenter = saveUser("commenter2@example.com", "commenter2");
        User liker = saveUser("liker2@example.com", "liker2");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("읽음 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"첫 댓글"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(liker.getId()))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn();
        printPrettyResponse(listResult);

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        long firstNotificationId = listJson.path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", firstNotificationId)
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].isRead").value(true))
                .andExpect(jsonPath("$.data.items[1].isRead").value(true));
    }

    @Test
    void markAsRead_forOtherUsersNotification_forbidden() throws Exception {
        User author = saveUser("author3@example.com", "author3");
        User commenter = saveUser("commenter3@example.com", "commenter3");
        User other = saveUser("other3@example.com", "other3");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.HELP_BRANCH)
                        .title("권한 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"권한 확인"}
                                """))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andReturn();
        printPrettyResponse(listResult);

        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        long notificationId = listJson.path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId)
                        .with(authentication(new JwtAuthentication(other.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(7002));
    }

    @Test
    void selfCommentAndLike_doNotCreateNotifications() throws Exception {
        User author = saveUser("self@example.com", "selfUser");
        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.SUPPORT_ROOT)
                        .title("셀프 알림 없음")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(author.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"내가 단 댓글"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void deleteNotification_success() throws Exception {
        User author = saveUser("delete-author@example.com", "deleteAuthor");
        User commenter = saveUser("delete-commenter@example.com", "deleteCommenter");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.TREE_SHADE)
                        .title("삭제 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"삭제 대상 알림"}
                                """))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andReturn();

        long notificationId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/v1/notifications/{notificationId}", notificationId)
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void deleteNotification_forOtherUsersNotification_forbidden() throws Exception {
        User author = saveUser("delete-author2@example.com", "deleteAuthor2");
        User commenter = saveUser("delete-commenter2@example.com", "deleteCommenter2");
        User other = saveUser("delete-other@example.com", "deleteOther");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.SUNNY_PLACE)
                        .title("삭제 권한 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"삭제 권한 확인"}
                                """))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        long notificationId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/v1/notifications/{notificationId}", notificationId)
                        .with(authentication(new JwtAuthentication(other.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(7002));
    }

    @Test
    void deleteAllUserNotifications_success() throws Exception {
        User author = saveUser("delete-all-author@example.com", "deleteAllAuthor");
        User commenter = saveUser("delete-all-commenter@example.com", "deleteAllCommenter");
        User liker = saveUser("delete-all-liker@example.com", "deleteAllLiker");

        CommunityPost post = communityPostRepository.save(
                CommunityPost.builder()
                        .user(author)
                        .category(CommunityCategory.HELP_BRANCH)
                        .title("전체 삭제 테스트")
                        .content("본문")
                        .build()
        );

        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", post.getId())
                        .with(authentication(new JwtAuthentication(commenter.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"전체 삭제 댓글"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/community/posts/{postId}/likes", post.getId())
                        .with(authentication(new JwtAuthentication(liker.getId()))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(new JwtAuthentication(author.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .password("encoded-password")
                        .nickname(nickname)
                        .build()
        );
    }

    private void printPrettyResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("==== API 응답 결과 ====");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json)));
    }
}
