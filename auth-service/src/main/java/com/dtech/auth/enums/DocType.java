package com.dtech.auth.enums;

public enum DocType implements DescribableEnum{
    BIRTH("Birth"),
    MARRIED("Married"),
    PROFILE("Profile");
    private final String description;
    DocType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
