package com.dtech.auth.appversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobileAppVersionAdviceTest {

    @Test
    void comparesNumericVersionSegmentsInsteadOfLexicographicText() {
        assertEquals(1, MobileAppVersionAdvice.compareVersions("1.10.0", "1.9.9"));
        assertEquals(-1, MobileAppVersionAdvice.compareVersions("1.2.9", "1.3.0"));
        assertEquals(0, MobileAppVersionAdvice.compareVersions("2.0", "2.0.0"));
    }

    @Test
    void ignoresBuildAndPreReleaseSuffixesForPolicyComparison() {
        assertEquals(0, MobileAppVersionAdvice.compareVersions("2.1.0-beta.1", "2.1.0"));
        assertEquals(0, MobileAppVersionAdvice.compareVersions("2.1.0+42", "2.1.0"));
    }
}
