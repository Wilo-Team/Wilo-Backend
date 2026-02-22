package com.wilo.server.community.dto;

import com.wilo.server.community.entity.CommunityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CommunityPostCreateRequestDto(
        @NotNull(message = "카테고리는 필수입니다.")
        CommunityCategory category,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 150, message = "제목은 150자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 10000, message = "내용은 10000자 이하여야 합니다.")
        String content,

        List<@Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.") String> imageUrls
) {
}
