package com.dtech.auth.enums;

public enum RequestType implements DescribableEnum {

    MARRIED("Married"),
    DIVORCE("Divorce");

    private final String description;

    RequestType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
