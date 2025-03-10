/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:09 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.dtech.auth.enums.DocType;
import com.dtech.auth.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupportingDocumentValidatorDTO {
    @NotBlank(message = "Image type is required.")
    @ValidEnum(enumClass = DocType.class, message = "Invalid image type.")
    private String type;
    @NotBlank(message = "File is required.")
    private String file;
}
