/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.claim.dto.request.validator;

import com.dtech.claim.model.DeathClaimDocTypes;
import com.dtech.claim.validator.ValidEnum;
import com.dtech.claim.validator.ValidFileType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeathSupportingDocumentValidatorDTO {
    @NotBlank(message = "Type is required.")
    @ValidEnum(enumClass = DeathClaimDocTypes.class, message = "Invalid type.")
    private String type;
    @NotBlank(message = "File is required.")
    private String file;
    @NotBlank(message = "File type is required.")
    @ValidFileType(message = "Only PNG, JPEG, JPG, and PDF file types are allowed.")
    private String fileType;
    @NotBlank(message = "File name is required.")
    private String fileName;
}
