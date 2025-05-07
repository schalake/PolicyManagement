package com.eana.insurance.utility;

import java.security.SecureRandom;

public class PolicyNumberGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 7; // 10 total: 3 for "POL" + 7 random
    private static final SecureRandom random = new SecureRandom();

    public static String generatePolicyNumber() {
        StringBuilder randomPart = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            randomPart.append(CHARACTERS.charAt(index));
        }
        return "POL" + randomPart;
    }
}
