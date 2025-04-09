/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:13 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InsuranceClaimsDetailsResponseDTO {
    private Long id;
    private TreatmentResponseDTO treatment;
    private TreatmentCategoryResponseDTO treatmentCategory;
    private Date fromTreatmentDate;
    private Date toTreatmentDate;
    private String disease;
    private List<DocumentDownloadResponseDTO> documents;
}
