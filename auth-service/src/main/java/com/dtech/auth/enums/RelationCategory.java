package com.dtech.auth.enums;

public enum RelationCategory implements DescribableEnum {
    MOTHER("Mother"),
    FATHER("Father"),
    CHILD("Child"),
    WIFE("Wife"),
    HUSBAND("Husband"),
    FATHER_IN_LAW("Father in law"),
    MOTHER_IN_LAW("Mother in law"),
    BROTHER("Brother"),
    SISTER("Sister");

    private final String description;

    RelationCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
