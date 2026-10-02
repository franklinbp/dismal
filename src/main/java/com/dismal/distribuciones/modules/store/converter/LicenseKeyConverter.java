package com.dismal.distribuciones.modules.store.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Converter
public class LicenseKeyConverter implements AttributeConverter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(LicenseKeyConverter.class);

    private static final String LEGACY_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String VERSION_PREFIX = "v2:";

    // The secret key is injected from application.properties
    private final Key key;

    public LicenseKeyConverter(@Value("${data.encryption.aes-key}") String secret) {
        this.key = new SecretKeySpec(Base64.getDecoder().decode(secret), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt license key", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            if (dbData.startsWith(VERSION_PREFIX)) {
                byte[] payload = Base64.getDecoder().decode(dbData.substring(VERSION_PREFIX.length()));
                byte[] iv = new byte[GCM_IV_BYTES];
                byte[] cipherText = new byte[payload.length - GCM_IV_BYTES];
                System.arraycopy(payload, 0, iv, 0, GCM_IV_BYTES);
                System.arraycopy(payload, GCM_IV_BYTES, cipherText, 0, cipherText.length);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
                return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
            }
            Cipher cipher = Cipher.getInstance(LEGACY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to decrypt license key from database; returning masked value", e);
            return "[LICENSE_UNREADABLE]";
        }
    }
}
