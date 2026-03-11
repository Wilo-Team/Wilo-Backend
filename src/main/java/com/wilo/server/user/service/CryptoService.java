package com.wilo.server.user.service;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";

    @Value("${spring.crypto.secret-key}")
    private String secretKey;

    // ✅ 암호화
    public String encrypt(String plainText) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // ✅ 복호화
    public String decrypt(String encryptedText) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }
}
