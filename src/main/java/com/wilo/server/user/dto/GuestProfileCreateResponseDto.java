package com.wilo.server.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestProfileCreateResponseDto {
    private String guestId;
    private String nickname;
    private Long chatbotTypeId;
}