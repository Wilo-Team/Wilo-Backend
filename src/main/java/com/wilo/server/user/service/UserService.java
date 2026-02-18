package com.wilo.server.user.service;

import com.wilo.server.files.service.FileService;
import com.wilo.server.files.exception.FileErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.dto.UserResponseDto;
import com.wilo.server.user.dto.UserRegisterDto;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.error.UserErrorCase;
import com.wilo.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateUserProfile(Long userId, UserRegisterDto request) {
        User user = getUserOrThrow(userId);

        if (!user.getNickname().equals(request.nickname()) && userRepository.existsByNickname(request.nickname())) {
            throw ApplicationException.from(UserErrorCase.NICKNAME_ALREADY_EXISTS);
        }

        user.updateProfile(request.nickname(), request.description());
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateProfileImage(Long userId, MultipartFile image) {
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

        return UserResponseDto.from(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApplicationException.from(UserErrorCase.USER_NOT_FOUND));
    }
}
