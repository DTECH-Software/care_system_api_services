/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 5:41 PM
 * <p>
 */

package com.dtech.login.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeUtil {

    public static Date getCurrentDateTime() {
        Instant instant = Instant.now();
        return Date.from(instant);
    }

    public static Date get30FutureDate() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(28);
        return Date.from(futureDate.atZone(ZoneId.systemDefault()).toInstant());
    }
}
