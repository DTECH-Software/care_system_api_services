package com.dtech.claim.enums;

public enum ClaimType {

    MEDICAL("Medical"),
    DEATH("Death donation fund");

    private final String description;

    ClaimType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
