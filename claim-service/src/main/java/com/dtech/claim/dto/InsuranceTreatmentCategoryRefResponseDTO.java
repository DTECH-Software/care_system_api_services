/**
 * User: Himal_J
 * Date: 4/8/2025
 * Time: 1:02 PM
 * <p>
 */

package com.dtech.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsuranceTreatmentCategoryRefResponseDTO {
    private String treatmentCategory;
    private List<AvailableInsuranceLimitDTO> limits;
}
