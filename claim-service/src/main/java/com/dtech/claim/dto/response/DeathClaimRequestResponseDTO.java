/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 2:53 PM
 * <p>
 */

package com.dtech.claim.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class DeathClaimRequestResponseDTO {
    private Long id;
    private String requestId;
    private Date deathDate;
    private String requestStatus;
    private String requestStatusDescription;
    private String remark;
    private String paymentType;
    private String paymentTypeDescription;
    private BigDecimal utilizeAmount;
    private ClaimsDependentsResponseDTO claimsDependents;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Colombo")
    private Date approvedDateTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Colombo")
    private Date createdDate;
    private String staffCategoryCode;
    private String staffCategoryDescription;
  //  private List<DocumentDownloadResponseDTO> documents;
}
