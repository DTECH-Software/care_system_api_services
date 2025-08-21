/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 9:20 AM
 * <p>
 */

package com.dtech.claim.model;

import com.dtech.claim.enums.DescribableEnum;

public enum DeathClaimDocTypes implements DescribableEnum {

    OTHER_CERTIFICATE("Other certificate"),
    DEATH_CERTIFICATE("Death certificate");

    private final String description;
    DeathClaimDocTypes(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
