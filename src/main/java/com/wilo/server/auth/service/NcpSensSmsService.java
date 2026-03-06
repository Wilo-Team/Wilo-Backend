package com.wilo.server.auth.service;

import com.wilo.server.auth.config.NcpSmsProperties;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class NcpSensSmsService {

    private static final String SENS_BASE_URL = "https://sens.apigw.ntruss.com";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final NcpSmsProperties ncpSmsProperties;

    public void sendVerificationCode(String toPhoneNumber, String verificationCode) {
        validateConfiguration();

        String timestamp = String.valueOf(System.currentTimeMillis());
        String requestPath = "/sms/v2/services/" + ncpSmsProperties.getServiceId() + "/messages";
        String signature = createSignature("POST", requestPath, timestamp, ncpSmsProperties.getAccessKey(), ncpSmsProperties.getSecretKey());

        String text = String.format("[Wilo] 인증번호는 %s 입니다. 3분 내에 입력해주세요.", verificationCode);
        Map<String, Object> payload = Map.of(
                "type", "SMS",
                "contentType", "COMM",
                "countryCode", "82",
                "from", ncpSmsProperties.getSenderPhone(),
                "content", text,
                "messages", List.of(
                        Map.of("to", toPhoneNumber)
                )
        );

        try {
            RestClient.create(SENS_BASE_URL)
                    .post()
                    .uri(requestPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", ncpSmsProperties.getAccessKey())
                    .header("x-ncp-apigw-signature-v2", signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("NCP SENS request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        } catch (RestClientException e) {
            log.warn("NCP SENS request failed. message={}", e.getMessage());
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(ncpSmsProperties.getAccessKey())
                || !StringUtils.hasText(ncpSmsProperties.getSecretKey())
                || !StringUtils.hasText(ncpSmsProperties.getServiceId())
                || !StringUtils.hasText(ncpSmsProperties.getSenderPhone())) {
            throw ApplicationException.from(AuthErrorCase.SMS_CONFIGURATION_MISSING);
        }
    }

    private String createSignature(
            String method,
            String requestPath,
            String timestamp,
            String accessKey,
            String secretKey
    ) {
        String message = method + " " + requestPath + "\n" + timestamp + "\n" + accessKey;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (GeneralSecurityException e) {
            throw ApplicationException.from(AuthErrorCase.SMS_SEND_FAILED);
        }
    }
}
