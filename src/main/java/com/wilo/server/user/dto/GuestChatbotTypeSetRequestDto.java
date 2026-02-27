package com.wilo.server.user.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class GuestChatbotTypeSetRequestDto {
    @NotNull(message = "chatbotTypeId는 필수입니다.")
    @Positive(message = "chatbotTypeId는 양수여야 합니다.")
    private Long chatbotTypeId;
}
