package com.dtech.auth.util;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateOnlyUtilTest {

    @Test
    void databaseDateDoesNotChangeWhenJvmTimezoneChanges() {
        TimeZone original = TimeZone.getDefault();
        try {
            Date dob = Date.valueOf("2001-01-11");
            for (String timezone : new String[]{"UTC", "Asia/Colombo", "America/Los_Angeles"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(timezone));
                assertEquals("2001-01-11", DateOnlyUtil.format(dob));
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
