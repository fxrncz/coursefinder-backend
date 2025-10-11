package com.app.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class PasswordUtil {
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256; // 256-bit derived key
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordUtil() {}

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return ITERATIONS + ":" + saltB64 + ":" + hashB64;
    }

    public static boolean verifyPassword(String plainPassword, String stored) {
        if (plainPassword == null || stored == null || stored.isEmpty()) {
            return false;
        }

        String[] parts = stored.split(":");
        if (parts.length != 3) {
            // Unknown format
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
        return slowEquals(expectedHash, actualHash);
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}


