package com.wilo.server.user.service;

import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.dto.GuestNicknameRequestDto;
import com.wilo.server.user.dto.GuestNicknameResponseDto;
import com.wilo.server.user.entity.GuestProfile;
import com.wilo.server.user.error.UserErrorCase;
import com.wilo.server.user.repository.GuestProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestProfileService {

    private final GuestProfileRepository guestProfileRepository;


    public GuestNicknameResponseDto setNickname(
            String guestIdHeader,
            GuestNicknameRequestDto request
    ) {

        String guestId = normalizeGuestId(guestIdHeader);

        if (guestId == null) {
            throw new ApplicationException(
                    ChatbotErrorCase.GUEST_ID_REQUIRED
            );
        }

        validateNickname(request.getNickname());

        GuestProfile profile = guestProfileRepository.findByGuestId(guestId).orElseGet(() -> guestProfileRepository.save(GuestProfile.create(guestId)));

        profile.updateNickname(request.getNickname());

        return GuestNicknameResponseDto.builder()
                .guestId(guestId)
                .nickname(profile.getNickname())
                .build();
    }


    private void validateNickname(String nickname) {

        if (nickname == null || nickname.isBlank()) {
            throw new ApplicationException(UserErrorCase.INVALID_NICKNAME);
        }

        if (nickname.length() > 20) {
            throw new ApplicationException(UserErrorCase.INVALID_NICKNAME);
        }

    }


    private String normalizeGuestId(String guestIdHeader) {
        if (guestIdHeader == null) return null;

        String trimmed = guestIdHeader.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

}
