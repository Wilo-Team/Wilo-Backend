package com.wilo.server.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
                .andExpect(jsonPath("$.data.user.email").value("signup@example.com"))
                .andExpect(jsonPath("$.data.user.nickname").value("signupUser"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
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
                .andExpect(jsonPath("$.errorCode").value(4091));
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
                .andExpect(jsonPath("$.data.user.nickname").value("loginUser"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void logout_success_clearsCookies() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andReturn();

        String setCookieHeader = String.join("\n", result.getResponse().getHeaders("Set-Cookie"));

        org.assertj.core.api.Assertions.assertThat(setCookieHeader).contains("accessToken=");
        org.assertj.core.api.Assertions.assertThat(setCookieHeader).contains("refreshToken=");
        org.assertj.core.api.Assertions.assertThat(setCookieHeader).contains("Max-Age=0");
    }
}
