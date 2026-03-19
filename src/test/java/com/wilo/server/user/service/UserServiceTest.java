package com.wilo.server.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wilo.server.files.service.FileService;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.dto.UserUpdateRequestDto;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.error.UserErrorCase;
import com.wilo.server.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserProfile_updatesOnlyNickname() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNickname")
                .description("oldDescription")
                .phoneNumber("01012345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNickname")).thenReturn(false);

        userService.updateUserProfile(1L, new UserUpdateRequestDto("newNickname", null));

        assertEquals("newNickname", user.getNickname());
        assertEquals("oldDescription", user.getDescription());
        assertEquals("01012345678", user.getPhoneNumber());
    }

    @Test
    void updateUserProfile_updatesOnlyDescription() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNickname")
                .description("oldDescription")
                .phoneNumber("01012345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserProfile(1L, new UserUpdateRequestDto(null, "newDescription"));

        verify(userRepository, never()).existsByNickname(anyString());
        assertEquals("oldNickname", user.getNickname());
        assertEquals("newDescription", user.getDescription());
        assertEquals("01012345678", user.getPhoneNumber());
    }

    @Test
    void updateUserProfile_updatesNicknameAndDescription() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNickname")
                .description("oldDescription")
                .phoneNumber("01012345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNickname")).thenReturn(false);

        userService.updateUserProfile(1L, new UserUpdateRequestDto("newNickname", "newDescription"));

        assertEquals("newNickname", user.getNickname());
        assertEquals("newDescription", user.getDescription());
        assertEquals("01012345678", user.getPhoneNumber());
    }

    @Test
    void updateUserProfile_throwsWhenBothNicknameAndDescriptionAreMissing() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNickname")
                .description("oldDescription")
                .phoneNumber("01012345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> userService.updateUserProfile(1L, new UserUpdateRequestDto(null, null))
        );

        assertEquals(UserErrorCase.INVALID_PROFILE_UPDATE_REQUEST, exception.getErrorCase());
    }

    @Test
    void updateUserProfile_throwsWhenNicknameAlreadyExists() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNickname")
                .description("oldDescription")
                .phoneNumber("01012345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNickname")).thenReturn(true);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> userService.updateUserProfile(1L, new UserUpdateRequestDto("newNickname", null))
        );

        assertEquals(UserErrorCase.NICKNAME_ALREADY_EXISTS, exception.getErrorCase());
    }
}
