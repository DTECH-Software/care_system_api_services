package com.dtech.auth.enums;

public enum DocType implements DescribableEnum{
    BIRTH("Birth"),
    MARRIED("Married"),
    PROFILE("Profile"),
    DIAGNOSIS_CARD("Diagnosis card"),
    TREATMENT_BILL("Treatment bill"),
    DEATH_CERTIFICATE("Death certificate");
    private final String description;
    DocType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
