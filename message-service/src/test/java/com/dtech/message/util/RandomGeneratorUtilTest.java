package com.dtech.message.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomGeneratorUtilTest {

    @Test
    void generatesExactlySixDigits() {
        for (int i = 0; i < 1_000; i++) {
            assertTrue(RandomGeneratorUtil.getRandom6DigitNumber().matches("\\d{6}"));
        }
    }
}
