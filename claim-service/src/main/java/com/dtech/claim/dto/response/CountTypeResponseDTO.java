/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 8:53 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountTypeResponseDTO {
    private long full;
    private long approved;
    private long rejected;
    private long underReview;
    private BigDecimal sumOfUtilizeAmount;
}
