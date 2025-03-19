package com.dtech.auth.enums;

public enum ProfileImageTypes implements DescribableEnum{

    PROFILE("Profile");

    private final String description;
    ProfileImageTypes(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
