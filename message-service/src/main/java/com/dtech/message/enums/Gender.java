/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 3:17 PM
 * <p>
 */

package com.dtech.message.enums;

public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
