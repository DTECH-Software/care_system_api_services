package com.dtech.message.enums;

public enum RelationCategory {
    MOTHER("Mother"),
    FATHER("Father"),
    CHILD("Child"),
    WIFE("Wife");

    private final String description;

    RelationCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
