/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 5:41 PM
 * <p>
 */

package com.dtech.login.util;

import lombok.extern.log4j.Log4j2;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    public static Date getSeconds(Date date,Integer amount) {
        log.info("get 60s DateTime");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, amount);
        return calendar.getTime();
    }

    public static String getOnlyTimeFormatter(Date date) {
        log.info("get time formatter");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(date);
    }

    public static long getMinutes(String time) {
        log.info("get minutes");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime givenDate = LocalDateTime.parse(time, formatter);
        Duration duration = Duration.between(givenDate, LocalDateTime.now());
        return duration.toMinutes();
    }


}
