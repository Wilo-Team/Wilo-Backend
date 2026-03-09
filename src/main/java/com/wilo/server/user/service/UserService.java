package com.wilo.server.user.service;

import com.wilo.server.files.service.FileService;
import com.wilo.server.files.exception.FileErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.dto.UserPasswordUpdateRequestDto;
import com.wilo.server.user.dto.UserResponseDto;
import com.wilo.server.user.dto.UserUpdateRequestDto;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.error.UserErrorCase;
import com.wilo.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateUserProfile(Long userId, UserUpdateRequestDto request) {
        User user = getUserOrThrow(userId);

        if (request.nickname() == null && request.description() == null) {
            throw ApplicationException.from(UserErrorCase.INVALID_PROFILE_UPDATE_REQUEST);
        }

        if (request.nickname() != null
                && !user.getNickname().equals(request.nickname())
                && userRepository.existsByNickname(request.nickname())) {
            throw ApplicationException.from(UserErrorCase.NICKNAME_ALREADY_EXISTS);
        }

        String nicknameToUpdate = request.nickname() == null ? user.getNickname() : request.nickname();
        String descriptionToUpdate = request.description() == null ? user.getDescription() : request.description();

        user.updateProfile(nicknameToUpdate, descriptionToUpdate, user.getPhoneNumber());
        return UserResponseDto.from(user);
    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw ApplicationException.from(FileErrorCase.FILE_UPLOAD_FAILED);
        }

        User user = getUserOrThrow(userId);

        String previousImageUrl = user.getProfileImageUrl();
        String uploadedImageUrl = fileService.uploadFileToS3(image);

        user.updateProfileImageUrl(uploadedImageUrl);

        if (previousImageUrl != null && !previousImageUrl.isBlank() && !previousImageUrl.equals(uploadedImageUrl)) {
            try {
                fileService.deleteImageFileFromS3(previousImageUrl);
            } catch (Exception e) {
                log.warn("기존 프로필 이미지 삭제 실패: {}", previousImageUrl, e);
            }
        }

        return uploadedImageUrl;
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequestDto request) {
        User user = getUserOrThrow(userId);

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApplicationException.from(UserErrorCase.USER_NOT_FOUND));
    }
}
