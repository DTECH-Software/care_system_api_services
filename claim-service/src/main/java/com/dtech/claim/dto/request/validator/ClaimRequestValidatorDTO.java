/**
 * User: Himal_J
 * Date: 3/5/2025
 * Time: 8:59 AM
 * <p>
 */

package com.dtech.claim.dto.request.validator;

import com.dtech.claim.validator.Conditional;
import com.dtech.claim.validator.ValidPastDays;
import com.dtech.claim.validator.ValidateDateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Conditional(selected = "isEmployee", values = {"false"}, required = {"claimsDependentId"}, message = "Claim dependent is required.")
@ValidateDateRange(message = "Treatment from date and to date invalid.")
public class ClaimRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Treatment is required.")
    private String treatment;
    @NotNull(message = "Request Amount is required.")
    @DecimalMin(value = "0.01", inclusive = true, message = "Request Amount must be greater than 0.")
    @Positive(message = "Request Amount must be a positive value.")
    private BigDecimal requestAmount;
    private String remark;
    @NotNull(message = "Claim request person type is required.")
    private Boolean isEmployee = true;
    private long claimsDependentId;
    @NotNull(message = "Treatment from date is required.")
    private Date fromDate;
    @NotNull(message = "Treatment to date is required.")
    @ValidPastDays(message = "Treatment must be past date")
    private Date toDate;
    @NotBlank(message = "Disease is required.")
    private String disease;
    @NotNull(message = "Patient document is required.")
    @NotEmpty(message = "Patient document is required.")
    @Valid
    private List<SupportingDocumentValidatorDTO> documents;
    @NotEmpty(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP length must be exactly 6")
    private String otp;
}
