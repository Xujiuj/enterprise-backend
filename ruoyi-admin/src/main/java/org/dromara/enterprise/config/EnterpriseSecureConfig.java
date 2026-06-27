package org.dromara.enterprise.config;

import org.springframework.boot.json.JsonParserFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local encrypted configuration used when enterprise-managed secrets are not available.
 */
public final class EnterpriseSecureConfig {

    public static final String DEFAULT_CONFIG_PATH = "config/secure-config.enc";
    public static final String DEFAULT_MASTER_KEY_PATH = "config/.master.key";
    public static final String VERSION = "fx-secure-config-v1";

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATIONS = 210_000;

    private EnterpriseSecureConfig() {
    }

    public static String generateMasterKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String encrypt(Map<String, String> properties, String masterKey) {
        try {
            byte[] salt = randomBytes(SALT_LENGTH);
            byte[] iv = randomBytes(IV_LENGTH);
            SecretKeySpec key = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(toJson(properties).getBytes(StandardCharsets.UTF_8));
            return String.join(".",
                VERSION,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(ciphertext)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt enterprise secure config", e);
        }
    }

    public static Map<String, Object> decrypt(String payload, String masterKey) {
        try {
            String[] parts = payload.trim().split("\\.");
            if (parts.length != 4 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported secure config format");
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] iv = Base64.getDecoder().decode(parts[2]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[3]);
            SecretKeySpec key = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            String json = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
            return JsonParserFactory.getJsonParser().parseMap(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt enterprise secure config", e);
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static SecretKeySpec deriveKey(String masterKey, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static String toJson(Map<String, String> properties) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : new LinkedHashMap<>(properties).entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(escape(entry.getKey())).append("\":\"")
                .append(escape(entry.getValue())).append('"');
            first = false;
        }
        json.append('}');
        return json.toString();
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }
}
