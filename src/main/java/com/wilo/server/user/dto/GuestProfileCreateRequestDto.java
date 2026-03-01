package com.wilo.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class GuestProfileCreateRequestDto {
    @NotBlank
    @Size(max = 20, message = "nickname은 최대 20자까지 가능합니다.")
    private String nickname;
    @NotNull @Positive
    private Long chatbotTypeId;
}