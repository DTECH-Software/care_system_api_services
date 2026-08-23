/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 10:15 AM
 * <p>
 */

package com.dtech.claim.dto.search;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ClaimHistory {
    private Date fromDate;
    private Date toDate;
    private String requestId;
    private BigDecimal requestAmount;
    private String requestStatus;
    private String claimsDependents;
    private String treatmentType;
    private String relationCategory;
}
