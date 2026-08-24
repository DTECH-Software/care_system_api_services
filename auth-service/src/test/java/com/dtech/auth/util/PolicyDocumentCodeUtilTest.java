package com.dtech.auth.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyDocumentCodeUtilTest {

    @Test
    void createsMedicalPolicyCodesUsingOnlyStaffCategory() {
        assertEquals(
                List.of("POLICY_INS_EX-OP2", "POLICY_INSEX-OP2"),
                PolicyDocumentCodeUtil.candidateCodes(true, "EX-OP2"));
    }

    @Test
    void createsDeathPolicyCodesUsingOnlyStaffCategory() {
        assertEquals(
                List.of("POLICY_DDF_NS", "POLICY_DDFNS"),
                PolicyDocumentCodeUtil.candidateCodes(false, "NS"));
    }

    @Test
    void returnsNoCodesWithoutStaffCategory() {
        assertTrue(PolicyDocumentCodeUtil.candidateCodes(true, " ").isEmpty());
    }
}
