/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 2:10 PM
 * <p>
 */

package com.dtech.auth.enums;

public enum DependentCategory implements DescribableEnum{
    PARENTS("Parents"),
    CHILDREN("Children"),
    WIFE("Wife"),
    HUSBAND("Husband"),
    FATHER_IN_LAW("Father in law"),
    MOTHER_IN_LAW("Mother in law"),
    BROTHER("Brother"),
    SISTER("Sister");

    private final String description;

    DependentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
