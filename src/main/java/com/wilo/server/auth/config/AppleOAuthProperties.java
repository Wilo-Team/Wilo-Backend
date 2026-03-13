package com.wilo.server.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "apple")
public class AppleOAuthProperties {

    private String clientId;
    private String teamId;
    private String keyId;
    private String privateKey;
    private String issuer = "https://appleid.apple.com";
    private String keysUri = "https://appleid.apple.com/auth/keys";
    private String tokenUri = "https://appleid.apple.com/auth/oauth2/v2/token";
    private String revokeUri = "https://appleid.apple.com/auth/oauth2/v2/revoke";
}
