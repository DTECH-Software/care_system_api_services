/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 4:27 PM
 * <p>
 */

package com.dtech.auth.util;

import lombok.extern.log4j.Log4j2;

import java.util.Random;

@Log4j2
public class RandomGeneratorUtil {

    public static String getRandom6DigitNumber() {
        try {
            log.error("called random 6 digit generation number");
            Random rnd = new Random();
            int number = rnd.nextInt(999999);
            return String.format("%06d", number);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
