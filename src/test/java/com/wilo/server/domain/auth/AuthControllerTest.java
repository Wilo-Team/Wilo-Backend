package com.wilo.server.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.auth.repository.RefreshTokenRepository;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signup_success() throws Exception {
        String request = """
                {
                  "email": "signup@example.com",
                  "password": "password1234",
                  "nickname": "signupUser",
                  "description": "hello",
                  "profileImageUrl": "https://example.com/me.png"
                }
                """;

                mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("success"))
                        .andExpect(jsonPath("$.data").value("회원가입이 완료되었습니다."));
    }

    @Test
    void signup_duplicateEmail_conflict() throws Exception {
        userRepository.save(User.builder()
                .email("dupe@example.com")
                .password(passwordEncoder.encode("password1234"))
                .nickname("nickname1")
                .description("bio")
                .profileImageUrl("https://example.com/profile.png")
                .build());

        String request = """
                {
                  "email": "dupe@example.com",
                  "password": "password1234",
                  "nickname": "nickname2",
                  "description": "hello",
                  "profileImageUrl": "https://example.com/me.png"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(1001));
    }

    @Test
    void login_success() throws Exception {
        userRepository.save(User.builder()
                .email("login@example.com")
                .password(passwordEncoder.encode("password1234"))
                .nickname("loginUser")
                .description("bio")
                .profileImageUrl("https://example.com/profile.png")
                .build());

        String request = """
                {
                  "email": "login@example.com",
                  "password": "password1234"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"a\",\"refreshToken\":\"r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));
    }
}
