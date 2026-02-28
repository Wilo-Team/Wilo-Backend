package com.wilo.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GuestProfileCreateRequestDto {
    @NotBlank
    private String nickname;
    @NotNull
    private Long chatbotTypeId;
}