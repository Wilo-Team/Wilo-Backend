package com.wilo.server.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.auth.config.AppleOAuthProperties;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AppleOAuthClient {

    private static final long JWKS_CACHE_TTL_MILLIS = 60L * 60L * 1000L;
    private static final long APPLE_CLIENT_SECRET_TTL_SECONDS = 60L * 60L * 24L * 30L;

    private final AppleOAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    private volatile Map<String, PublicKey> cachedPublicKeys = Map.of();
    private volatile long cachedAtMillis = 0L;

    public AppleIdentityClaims verifyAccessToken(String accessToken) {
        validateAppleConfig();

        try {
            String[] jwtParts = accessToken.split("\\.");
            if (jwtParts.length != 3) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(jwtParts[0]),
                    new TypeReference<>() {}
            );
            Object kidValue = header.get("kid");
            String kid = kidValue instanceof String ? (String) kidValue : null;
            if (kid == null || kid.isBlank()) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            PublicKey publicKey = resolvePublicKey(kid);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            if (!properties.getIssuer().equals(claims.getIssuer())) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }
            if (!properties.getClientId().equals(claims.getAudience())) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            return new AppleIdentityClaims(subject, claims.get("email", String.class));
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID, e);
        }
    }

    public void revokeByAuthorizationCode(String authorizationCode) {
        validateAppleConfig();
        String refreshToken = exchangeAuthorizationCode(authorizationCode);
        revokeRefreshToken(refreshToken);
    }

    public record AppleIdentityClaims(String subject, String email) {
    }

    private PublicKey resolvePublicKey(String kid) {
        long now = System.currentTimeMillis();
        if (cachedPublicKeys.isEmpty() || now - cachedAtMillis > JWKS_CACHE_TTL_MILLIS) {
            refreshPublicKeys();
        }

        PublicKey cachedKey = cachedPublicKeys.get(kid);
        if (cachedKey != null) {
            return cachedKey;
        }

        refreshPublicKeys();
        PublicKey refreshedKey = cachedPublicKeys.get(kid);
        if (refreshedKey == null) {
            throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
        }
        return refreshedKey;
    }

    private synchronized void refreshPublicKeys() {
        try {
            WebClient webClient = webClientBuilder.build();
            JsonNode response = webClient.get()
                    .uri(properties.getKeysUri())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("keys")) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            Map<String, PublicKey> keyMap = new HashMap<>();
            for (JsonNode keyNode : response.get("keys")) {
                String kid = keyNode.path("kid").asText("");
                String n = keyNode.path("n").asText("");
                String e = keyNode.path("e").asText("");
                if (kid.isBlank() || n.isBlank() || e.isBlank()) {
                    continue;
                }

                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
                keyMap.put(kid, keyFactory.generatePublic(keySpec));
            }

            if (keyMap.isEmpty()) {
                throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
            }

            cachedPublicKeys = Map.copyOf(keyMap);
            cachedAtMillis = System.currentTimeMillis();
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID, e);
        }
    }

    private String exchangeAuthorizationCode(String authorizationCode) {
        try {
            WebClient webClient = webClientBuilder.build();
            String clientSecret = generateClientSecret();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", authorizationCode);
            formData.add("client_id", properties.getClientId());
            formData.add("client_secret", clientSecret);

            JsonNode response = webClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.hasNonNull("refresh_token")) {
                throw ApplicationException.from(AuthErrorCase.APPLE_TOKEN_EXCHANGE_FAILED);
            }

            return response.get("refresh_token").asText();
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.APPLE_TOKEN_EXCHANGE_FAILED, e);
        }
    }

    private void revokeRefreshToken(String refreshToken) {
        try {
            WebClient webClient = webClientBuilder.build();
            String clientSecret = generateClientSecret();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", properties.getClientId());
            formData.add("client_secret", clientSecret);
            formData.add("token", refreshToken);
            formData.add("token_type_hint", "refresh_token");

            webClient.post()
                    .uri(properties.getRevokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.APPLE_TOKEN_REVOKE_FAILED, e);
        }
    }

    private String generateClientSecret() {
        try {
            ECPrivateKey privateKey = (ECPrivateKey) parsePrivateKey(properties.getPrivateKey());
            Instant now = Instant.now();

            return Jwts.builder()
                    .setHeaderParam("kid", properties.getKeyId())
                    .setHeaderParam("alg", "ES256")
                    .setIssuer(properties.getTeamId())
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(APPLE_CLIENT_SECRET_TTL_SECONDS)))
                    .setAudience(properties.getIssuer())
                    .setSubject(properties.getClientId())
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(AuthErrorCase.APPLE_CONFIG_MISSING, e);
        }
    }

    private PrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        if (pem == null || pem.isBlank()) {
            throw ApplicationException.from(AuthErrorCase.APPLE_CONFIG_MISSING);
        }

        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }

    private void validateAppleConfig() {
        if (isBlank(properties.getClientId())
                || isBlank(properties.getTeamId())
                || isBlank(properties.getKeyId())
                || isBlank(properties.getPrivateKey())) {
            throw ApplicationException.from(AuthErrorCase.APPLE_CONFIG_MISSING);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
