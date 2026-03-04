package com.wilo.server.auth.service;

import com.wilo.server.auth.config.SolapiProperties;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SolapiSmsService {

    private static final String SOLAPI_BASE_URL = "https://api.solapi.com";
    private static final String SEND_SMS_PATH = "/messages/v4/send-many/detail";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String AUTH_SCHEME = "HMAC-SHA256";

    private final SolapiProperties solapiProperties;

    public void sendVerificationCode(String toPhoneNumber, String verificationCode) {
        validateConfiguration();

        String date = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = createSignature(
                solapiProperties.getSecretKey(),
                date + salt
        );

        String authorizationHeader = String.format(
                "%s apiKey=%s, date=%s, salt=%s, signature=%s",
                AUTH_SCHEME,
                solapiProperties.getAccessKey(),
                date,
                salt,
                signature
        );

        String text = String.format("[Wilo] 인증번호는 %s 입니다. 3분 내에 입력해주세요.", verificationCode);
        Map<String, Object> payload = Map.of(
                "messages", java.util.List.of(Map.of(
                        "to", toPhoneNumber,
                        "from", solapiProperties.getSenderPhone(),
                        "text", text
                ))
        );

        try {
            RestClient.create(SOLAPI_BASE_URL)
                    .post()
                    .uri(SEND_SMS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorizationHeader)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Solapi request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        } catch (RestClientException e) {
            log.warn("Solapi request failed. message={}", e.getMessage());
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(solapiProperties.getAccessKey())
                || !StringUtils.hasText(solapiProperties.getSecretKey())
                || !StringUtils.hasText(solapiProperties.getSenderPhone())) {
            throw ApplicationException.from(AuthErrorCase.SOLAPI_CONFIGURATION_MISSING);
        }
    }

    private String createSignature(String secretKey, String valueToSign) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(valueToSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (GeneralSecurityException e) {
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        }
    }
}
