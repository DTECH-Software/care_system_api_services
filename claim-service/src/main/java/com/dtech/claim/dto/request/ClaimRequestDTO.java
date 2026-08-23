/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:42 AM
 * <p>
 */

package com.dtech.claim.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClaimRequestDTO extends ChannelRequestDTO {
    private String treatment;
    private String treatmentCategory;
    private BigDecimal requestAmount;
    private String remark;
    private long claimsDependentId;
    private Boolean isEmployee = true;
    private Date fromDate;
    private Date toDate;
    private String disease;
    private List<SupportingDocumentDTO> documents;
    private String otp;
    private Boolean isValidation;
}
