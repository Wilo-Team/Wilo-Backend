package com.wilo.server.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestNicknameResponseDto {
    private String guestId;
    private String nickname;
}
