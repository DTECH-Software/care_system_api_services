/**
 * User: Himal_J
 * Date: 3/18/2025
 * Time: 3:25 PM
 * <p>
 */

package com.dtech.claim.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AvailableInsuranceLimitDTO {
    private String treatment;
    private BigDecimal availableLimit;
    private BigDecimal fundLimit;
}
