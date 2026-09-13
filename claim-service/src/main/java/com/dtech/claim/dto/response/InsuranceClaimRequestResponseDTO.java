/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:10 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


@Data
public class InsuranceClaimRequestResponseDTO {
    private Long id;
    private String requestId;
    private BigDecimal requestAmount;
    private String requestStatus;
    private String requestStatusDescription;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Colombo")
    private Date approvedDateTime;
    private ClaimsDependentsResponseDTO claimsDependents;
    private InsuranceClaimsDetailsResponseDTO insuranceClaimsDetails;
    private BigDecimal approvedAmount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Colombo")
    private Date createdDate;
    private String staffCategoryCode;
    private String staffCategoryDescription;
}
