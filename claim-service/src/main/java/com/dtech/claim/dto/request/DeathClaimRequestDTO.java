/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 10:14 AM
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
public class DeathClaimRequestDTO extends ChannelRequestDTO{
    private String requestId;
    private String remark;
    private long claimsDependentId;
    private Date deathDate;
    private List<SupportingDocumentDTO> documents;
    private String otp;
}
