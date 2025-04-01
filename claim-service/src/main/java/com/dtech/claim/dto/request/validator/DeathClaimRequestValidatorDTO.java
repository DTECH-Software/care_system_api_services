/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 10:18 AM
 * <p>
 */

package com.dtech.claim.dto.request.validator;


import com.dtech.claim.validator.Conditional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "isValidation",
        values = {"false"
        }, required = {"otp"}, message = "OTP is required.")
public class DeathClaimRequestValidatorDTO extends ChannelRequestValidatorDTO{
    private String remark;
    @NotNull(message = "Claim dependent is required.")
    private long claimsDependentId;
    @NotNull(message = "Death date is required.")
    private Date deathDate;
    @NotNull(message = "Death certificate is required.")
    @NotEmpty(message = "Death certificate document is required.")
    @Valid
    private List<DeathSupportingDocumentValidatorDTO> documents;
    @Size(min = 6, max = 6, message = "OTP length must be exactly 6")
    private String otp;
    @NotNull(message = "Request validation type is required.")
    private Boolean isValidation;
}
