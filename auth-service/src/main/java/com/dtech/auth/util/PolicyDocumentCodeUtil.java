package com.dtech.auth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyDocumentCodeUtil {

    private static final String MEDICAL_POLICY_PREFIX = "POLICY_INS";
    private static final String DEATH_POLICY_PREFIX = "POLICY_DDF";

    public static List<String> candidateCodes(Boolean medicalPolicy, String staffCategoryCode) {
        if (staffCategoryCode == null || staffCategoryCode.isBlank()) {
            return List.of();
        }

        String prefix = Boolean.TRUE.equals(medicalPolicy)
                ? MEDICAL_POLICY_PREFIX
                : DEATH_POLICY_PREFIX;
        String normalizedStaffCategory = staffCategoryCode.trim();

        return List.of(
                prefix + "_" + normalizedStaffCategory,
                prefix + normalizedStaffCategory
        );
    }
}
