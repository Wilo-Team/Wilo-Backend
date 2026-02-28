package com.wilo.server.user.service;

import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.dto.*;
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
    private final ChatbotTypeRepository chatbotTypeRepository;


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

    public GuestChatbotTypeSetResponseDto setChatbotType(
            String guestIdHeader,
            GuestChatbotTypeSetRequestDto request
    ) {
        String guestId = normalizeGuestId(guestIdHeader);

        if (guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        var chatbotType = chatbotTypeRepository.findById(request.getChatbotTypeId())
                .filter(type -> type.isActive())
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.CHATBOT_TYPE_NOT_FOUND));

        GuestProfile profile = guestProfileRepository.findByGuestId(guestId)
                .orElseGet(() -> guestProfileRepository.save(GuestProfile.create(guestId)));

        profile.updateChatbotTypeId(chatbotType.getId());

        return GuestChatbotTypeSetResponseDto.builder()
                .chatbotTypeId(profile.getChatbotTypeId())
                .build();
    }

    public GuestProfileCreateResponseDto createProfile(
            String guestIdHeader,
            GuestProfileCreateRequestDto request
    ) {
        String guestId = normalizeGuestId(guestIdHeader);

        if (guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        validateNickname(request.getNickname());

        var chatbotType = chatbotTypeRepository.findById(request.getChatbotTypeId())
                .filter(type -> type.isActive())
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.CHATBOT_TYPE_NOT_FOUND));

        GuestProfile profile =
                guestProfileRepository.findByGuestId(guestId)
                        .orElseGet(() -> guestProfileRepository.save(GuestProfile.create(guestId)));

        profile.updateNickname(request.getNickname());
        profile.updateChatbotTypeId(chatbotType.getId());

        return GuestProfileCreateResponseDto.builder()
                .guestId(profile.getGuestId())
                .nickname(profile.getNickname())
                .chatbotTypeId(profile.getChatbotTypeId())
                .build();
    }

    private String normalizeGuestId(String guestIdHeader) {
        if (guestIdHeader == null) return null;
        String trimmed = guestIdHeader.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateNickname(String nickname) {

        if (nickname == null || nickname.isBlank()) {
            throw new ApplicationException(UserErrorCase.INVALID_NICKNAME);
        }

        if (nickname.length() > 20) {
            throw new ApplicationException(UserErrorCase.INVALID_NICKNAME);
        }
    }
}
