package com.dtech.auth.enums;

public enum NotificationTitle implements DescribableEnum{

    CLAIM_UPDATE("Claims update"),;

    private final String description;
    NotificationTitle(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
