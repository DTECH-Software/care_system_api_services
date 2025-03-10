package com.dtech.auth.enums;

public enum RelationCategory implements DescribableEnum {
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
