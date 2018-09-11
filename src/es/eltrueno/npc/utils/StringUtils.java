package es.eltrueno.npc.utils;

import java.security.SecureRandom;
import java.util.Random;

public class StringUtils {

    private static final Random RANDOM = new SecureRandom();
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String getRandomString() {
        return generateNumbers(9, CHARS.length);
    }

    private static String generateNumbers(int length, int maxIndex) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(CHARS[RANDOM.nextInt(maxIndex)]);
        }
        return sb.toString();
    }

}
