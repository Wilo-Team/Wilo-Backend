package com.wilo.server.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDto(
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 120, message = "한 줄 소개는 120자 이하여야 합니다.")
        String description
) {

    @AssertTrue(message = "닉네임 또는 한 줄 소개 중 하나는 입력해야 합니다.")
    public boolean isAtLeastOneFieldProvided() {
        return nickname != null || description != null;
    }
}
