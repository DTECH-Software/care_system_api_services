/**
 * User: Himal_J
 * Date: 3/18/2025
 * Time: 3:25 PM
 * <p>
 */

package com.dtech.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailableInsuranceLimitDTO {
    private BigDecimal availableLimit;
    private BigDecimal fundLimit;
}
