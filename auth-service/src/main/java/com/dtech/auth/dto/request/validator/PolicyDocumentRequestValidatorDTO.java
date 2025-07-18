package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class PolicyDocumentRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotNull(message = "Policy is required")
    private Boolean policy;
}
