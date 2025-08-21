package com.dtech.claim.enums;

public enum DocType implements DescribableEnum{
    BIRTH("Birth"),
    MARRIED("Married"),
    PROFILE("Profile"),
    DIAGNOSIS_CARD("Diagnosis card"),
    DEATH_CERTIFICATE("Death certificate"),
    OTHER_CERTIFICATE("Other certificate"),
    TREATMENT_BILL("Treatment bill");
    private final String description;
    DocType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
