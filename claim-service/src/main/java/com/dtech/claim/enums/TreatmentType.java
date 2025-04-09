/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 9:26 PM
 * <p>
 */

package com.dtech.claim.enums;

public enum TreatmentType {

    INDOOR("Indoor"),
    OUTPATIENT("Outpatient"),
    CRIC("Critical care"),
    DEATH("Death fund");

    private final String description;

    TreatmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
