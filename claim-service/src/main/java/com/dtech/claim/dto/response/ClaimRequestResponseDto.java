/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:10 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import com.dtech.claim.enums.Workflow;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClaimRequestResponseDto {
    private Long id;
    private String requestId;
    private BigDecimal requestAmount;
    private Workflow requestStatus;
    private String remark;
    private ClaimsDependentsResponseDto claimsDependents;
    private InsuranceClaimsDetailsResponseDto insuranceClaimsDetails;
}
