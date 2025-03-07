package com.dtech.message.enums;

public enum Title {
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
