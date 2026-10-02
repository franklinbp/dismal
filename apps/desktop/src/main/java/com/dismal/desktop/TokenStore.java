package com.dismal.desktop;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;

public class TokenStore {

    private static final String KEY_TOKEN = "dismal.token.data";
    private static final String KEY_IV = "dismal.token.iv";
    private static final String KEY_SALT = "dismal.token.salt";
    private static final String KEY_REMEMBER = "dismal.remember";
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 12000;

    private final Preferences preferences = Preferences.userNodeForPackage(TokenStore.class);

    public void saveToken(String token, boolean remember) {
        preferences.putBoolean(KEY_REMEMBER, remember);
        if (!remember || token == null || token.isBlank()) {
            clear();
            return;
        }
        try {
            byte[] salt = getOrCreateSalt();
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            byte[] encrypted = encrypt(token, deriveKey(salt), iv);
            preferences.put(KEY_TOKEN, Base64.getEncoder().encodeToString(encrypted));
            preferences.put(KEY_IV, Base64.getEncoder().encodeToString(iv));
        } catch (Exception ex) {
            clear();
        }
    }

    public String loadToken() {
        boolean remember = preferences.getBoolean(KEY_REMEMBER, false);
        if (!remember) {
            return null;
        }
        String data = preferences.get(KEY_TOKEN, null);
        String ivEncoded = preferences.get(KEY_IV, null);
        if (data == null || data.isBlank() || ivEncoded == null || ivEncoded.isBlank()) {
            return null;
        }
        try {
            byte[] salt = getOrCreateSalt();
            byte[] iv = Base64.getDecoder().decode(ivEncoded);
            byte[] cipherBytes = Base64.getDecoder().decode(data);
            return decrypt(cipherBytes, deriveKey(salt), iv);
        } catch (Exception ex) {
            clear();
            return null;
        }
    }

    public void clear() {
        preferences.remove(KEY_TOKEN);
        preferences.remove(KEY_IV);
        preferences.putBoolean(KEY_REMEMBER, false);
    }

    private byte[] getOrCreateSalt() {
        String existing = preferences.get(KEY_SALT, null);
        if (existing != null && !existing.isBlank()) {
            return Base64.getDecoder().decode(existing);
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        preferences.put(KEY_SALT, Base64.getEncoder().encodeToString(salt));
        return salt;
    }

    private SecretKey deriveKey(byte[] salt) throws Exception {
        String seed = System.getProperty("user.name") + "|" + System.getProperty("os.name") + "|" + System.getProperty("user.home");
        PBEKeySpec spec = new PBEKeySpec(seed.toCharArray(), salt, ITERATIONS, KEY_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] encrypt(String value, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decrypt(byte[] value, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] plain = cipher.doFinal(value);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
