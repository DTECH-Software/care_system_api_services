package com.dtech.claim.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssistedClaimControllerTest {

    @Test
    void shouldHideSystemGeneratedPrimaryMobile() {
        assertNull(AssistedClaimController.mobileForResponse("0000000038", false));
    }

    @Test
    void shouldReturnRealPrimaryMobile() {
        assertEquals(
                "0776037678",
                AssistedClaimController.mobileForResponse("0776037678", true)
        );
    }
}
