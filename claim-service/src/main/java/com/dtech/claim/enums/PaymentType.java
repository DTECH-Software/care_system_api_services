/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 7:53 AM
 * <p>
 */

package com.dtech.claim.enums;

public enum PaymentType implements DescribableEnum{

    FULL("Full"),
    HALF("Half");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
