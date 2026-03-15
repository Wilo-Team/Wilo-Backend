package com.wilo.server.auth.service;

import com.wilo.server.auth.dto.KakaoUserInfoResponseDto;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final WebClient.Builder webClientBuilder;

    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {

        try {
            KakaoUserInfoResponseDto response = webClientBuilder.build()
                    .get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfoResponseDto.class)
                    .block();

            if (response == null || response.id() == null) {
                throw ApplicationException.from(AuthErrorCase.INVALID_KAKAO_AUTH);
            }

            return response;

        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.KAKAO_API_COMMUNICATION_FAILED, e);
        }
    }
}
