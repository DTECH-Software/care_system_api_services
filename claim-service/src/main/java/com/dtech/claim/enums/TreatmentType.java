/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 9:26 PM
 * <p>
 */

package com.dtech.claim.enums;

public enum TreatmentType {

    INDOOR("Indoor"),
    OUTDOOR("Outpatient"),
    CRIC("Critical illness"),
    LIFC("Life cover"),
    ACCD("Accidental death"),
    TPPD("Total & premanent disability"),
    PPPD("Partial & premanent disability"),
    DEATH("Death fund");

    private final String description;

    TreatmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
