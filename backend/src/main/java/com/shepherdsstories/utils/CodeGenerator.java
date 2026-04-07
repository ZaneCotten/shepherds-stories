package com.shepherdsstories.utils;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    private CodeGenerator() {
    }

    // Generates a random alphanumeric string for missionary reference codes
    public static String generateReference(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }
}