/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 4:27 PM
 * <p>
 */

package com.dtech.login.util;

import java.security.SecureRandom;

public class RandomGeneratorUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomGeneratorUtil() {
    }

    public static String getRandom6DigitNumber() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
