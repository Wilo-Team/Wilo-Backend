package com.wilo.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GuestNicknameRequestDto {
    @NotBlank
    private String nickname;
}
