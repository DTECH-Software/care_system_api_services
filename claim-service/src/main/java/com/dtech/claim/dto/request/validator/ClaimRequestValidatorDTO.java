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
@Conditional(selected = "treatment", values = {"INDOOR","CRIC"}, required = {"fromDate"}, message = "Treatment from date is required.")
//@Conditional(selected = "treatmentCategory", values = {"OTHER"}, required = {"disease"}, message = "Disease is required.")
@ValidateDateRange(selected = "treatment", values = {"INDOOR","CRIC"}, message = "Treatment from date and to date invalid.")
@Conditional(selected = "isValidation", values = {"false"}, required = {"otp"}, message = "OTP is required.")
public class ClaimRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Treatment is required.")
    private String treatment;
    @NotBlank(message = "Treatment category is required.")
    private String treatmentCategory;
    @NotNull(message = "Request Amount is required.")
    @DecimalMin(value = "0.01", inclusive = true, message = "Request Amount must be greater than 0.")
    @Positive(message = "Request Amount must be a positive value.")
    private BigDecimal requestAmount;
    private String remark;
    @NotNull(message = "Claim request person type is required.")
    private Boolean isEmployee = true;
    private long claimsDependentId;
    private Date fromDate;
    @NotNull(message = "Treatment to date is required.")
    @ValidPastDays(message = "Treatment must be past date")
    private Date toDate;
    private String disease;
    @NotNull(message = "Patient document is required.")
    @NotEmpty(message = "Patient document is required.")
    @Valid
    private List<InsuranceSupportingDocumentValidatorDTO> documents;
    @Size(min = 6, max = 6, message = "OTP length must be exactly 6")
    private String otp;
    @NotNull(message = "Request validation type is required.")
    private Boolean isValidation;
    private String assistedMobileNo;

    @AssertTrue(message = "Invalid assisted mobile number. It must start with 071, 074, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    public boolean isAssistedMobileNoValid() {
        if (!Boolean.TRUE.equals(getAssistedMode()) || assistedMobileNo == null || assistedMobileNo.trim().isEmpty()) {
            return true;
        }
        return assistedMobileNo.trim().matches("^(071|070|074|077|075|078|072|076)[0-9]{7}$");
    }
}
