/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 7:39 AM
 * <p>
 */

package com.dtech.login.util;

import lombok.extern.log4j.Log4j2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Predicate;

@Log4j2
public class PasswordUtil {


    public static String passwordEncoder(String saltKey, String password) throws NoSuchAlgorithmException {
        try {
            log.info("Password Encoder {}", password);
            String passwordWithSalt = saltKey+password;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(passwordWithSalt.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static int countCharsByConditions(String str, Predicate<Character> predicate) {
        log.info("called count by char method {}", str);
        try {
            int count = 0;
            for(Character c : str.toCharArray() ) {
                if(predicate.test(c)) {
                    count++;
                }
            }
            return count;
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    public static int getCharCount(String str) {
        log.info("called count by get char count method {}", str);
        try {
            int count = 0;
            for(Character c : str.toCharArray() ) {
                    count++;
            }
            return count;
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


}
