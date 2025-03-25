/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 7:59 AM
 * <p>
 */

package com.dtech.claim.enums;

public enum Workflow {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ACTIVE("Active");

    private final String description;

    Workflow(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
