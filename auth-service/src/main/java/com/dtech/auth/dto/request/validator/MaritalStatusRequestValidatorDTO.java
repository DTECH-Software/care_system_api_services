package com.dtech.auth.dto.request.validator;

import com.dtech.auth.enums.RequestType;
import com.dtech.auth.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaritalStatusRequestValidatorDTO extends ChannelRequestValidatorDTO {
    @NotBlank(message = "Request type is required")
    @ValidEnum(message = "Invalid request type is required",enumClass = RequestType.class)
    private String requestType;
    @NotNull(message = "Supporting document is required.")
    @NotEmpty(message = "Supporting document is required.")
    @Valid
    private List<SupportingDocumentValidatorDTO> documents;
}
