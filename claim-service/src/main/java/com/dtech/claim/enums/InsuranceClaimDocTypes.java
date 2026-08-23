/**
 * User: Himal_J
 * Date: 3/19/2025
 * Time: 10:22 AM
 * <p>
 */

package com.dtech.claim.enums;

public enum InsuranceClaimDocTypes implements DescribableEnum{

    DIAGNOSIS_CARD("Diagnosis card"),
    TREATMENT_BILL("Treatment bill");

    private final String description;
    InsuranceClaimDocTypes(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
