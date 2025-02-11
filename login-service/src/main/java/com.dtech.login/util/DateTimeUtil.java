/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 5:41 PM
 * <p>
 */

package com.dtech.login.util;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
@Log4j2
public class DateTimeUtil {

    public static Date getCurrentDateTime() {
        log.info("get Current DateTime");
        Instant instant = Instant.now();
        return Date.from(instant);
    }

    public static Date get30FutureDate() {
        log.info("get 30 future DateTime");
        LocalDateTime futureDate = LocalDateTime.now().plusDays(28);
        return Date.from(futureDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date get60s() {
        Date currentDate = getCurrentDateTime();
        log.info("get 60s DateTime");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.SECOND, 60);
        return calendar.getTime();
    }
}
