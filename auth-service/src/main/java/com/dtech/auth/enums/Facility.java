/**
 * User: Himal_J
 * Date: 3/17/2025
 * Time: 10:41 AM
 * <p>
 */

package com.dtech.auth.enums;

public enum Facility implements DescribableEnum{
    INSURANCE("Insurance"),
    DEATH("Death Donation Funds"),
    BOTH("Both");

    private final String description;

    Facility(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
