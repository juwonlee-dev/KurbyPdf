package io.github.juwonlee.kurbypdf.util;

import java.security.SecureRandom;

public class KeyUtil {
    private static final SecureRandom RND = new SecureRandom();
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT = "0123456789";
    private static final String SYMBOL = "!@#$%^&*()-_=+[]{};:,.<>?";
    private static final String ALL = UPPER + LOWER + DIGIT + SYMBOL;

    public static String randomOwnerPassword() {
        return randomOwnerPassword(32); // Default: 32
    }

    public static String randomOwnerPassword(int len) {
        StringBuilder sb = new StringBuilder(len);

        // 각 그룹에서 최소 하나 보장
        sb.append(UPPER.charAt(RND.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RND.nextInt(LOWER.length())));
        sb.append(DIGIT.charAt(RND.nextInt(DIGIT.length())));
        sb.append(SYMBOL.charAt(RND.nextInt(SYMBOL.length())));

        // 나머지 랜덤 채움
        for (int i = 4; i < len; i++) sb.append(ALL.charAt(RND.nextInt(ALL.length())));

        // 섞기 (Fisher-Yates)
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RND.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }

        return new String(chars);
    }
}
