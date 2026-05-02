package com.guidapixel.contable.auth.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${afip.encryption.key:GuidaContable2026SecureKey!}")
    private String secretKey;

    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(padOrTrimKey(secretKey.getBytes()), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting AFIP cert password", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(padOrTrimKey(secretKey.getBytes()), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting AFIP cert password", e);
        }
    }

    private byte[] padOrTrimKey(byte[] key) {
        byte[] result = new byte[16];
        if (key.length <= 16) {
            System.arraycopy(key, 0, result, 0, key.length);
        } else {
            System.arraycopy(key, 0, result, 0, 16);
        }
        return result;
    }
}
