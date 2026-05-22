package com.boonya.lab.io.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String generateToken() {
        return generateToken(32);
    }

    public static String generateDeviceToken() {
        return "token_" + generateToken(24);
    }
}
