package com.dtech.claim.dto.request.validator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotEmpty(message = "Mobile number is required.")
    @Pattern(regexp = "^(071|070|077|075|078|072|076)[0-9]{7}$", message = "Invalid mobile number. It must start with 071, 070, 077, 075, 078, 072, or 076, and be followed by 7 digits.")
    private String primaryMobile;

}