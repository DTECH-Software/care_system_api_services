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
public class InsuranceClaimsDetailsResponseDto {
    private Long id;
    private TreatmentResponseDto treatment;
    private Date fromTreatmentDate;
    private Date toTreatmentDate;
    private String disease;
    private List<DocumentDownloadResponseDTO> documents;
}
