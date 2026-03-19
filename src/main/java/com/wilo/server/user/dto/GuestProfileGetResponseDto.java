package com.wilo.server.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestProfileGetResponseDto {
    private String guestId;
    private String nickname;
    private Long chatbotTypeId;
    private boolean profileExists;
}