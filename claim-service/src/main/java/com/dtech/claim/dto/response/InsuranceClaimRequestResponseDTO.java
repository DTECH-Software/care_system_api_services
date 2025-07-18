/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:10 AM
 * <p>
 */

package com.dtech.claim.dto.response;

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
    private Date approvedDateTime;
    private ClaimsDependentsResponseDTO claimsDependents;
    private InsuranceClaimsDetailsResponseDTO insuranceClaimsDetails;
    private BigDecimal approvedAmount;
}
