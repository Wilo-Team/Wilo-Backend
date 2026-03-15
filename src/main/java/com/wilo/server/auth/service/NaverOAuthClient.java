package com.wilo.server.auth.service;

import com.wilo.server.auth.dto.NaverUserInfoResponseDto;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class NaverOAuthClient {
    private static final String USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";
    private final WebClient.Builder webClientBuilder;

    public NaverUserInfoResponseDto getUserInfo(String accessToken) {
        try {
            NaverUserInfoResponseDto response =
                    webClientBuilder.build()
                            .get()
                            .uri(USER_INFO_URI)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(NaverUserInfoResponseDto.class)
                            .block();

            if (response == null || response.id() == null) {
                throw ApplicationException.from(AuthErrorCase.INVALID_NAVER_AUTH);
            }

            return response;

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.NAVER_API_COMMUNICATION_FAILED, e);
        }
    }
}
