package com.dtech.claim.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssistedEmployeeSelectValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "EPF number is required.")
    private String epfNo;
}
