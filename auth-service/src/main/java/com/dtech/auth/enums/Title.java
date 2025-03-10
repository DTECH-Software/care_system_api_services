package com.dtech.auth.enums;

public enum Title implements DescribableEnum {
    MR("Mr"),
    MRS("Mrs"),
    MISS("Miss"),
    MS("Ms");

    private final String description;

    Title(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
