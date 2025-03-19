/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:09 AM
 * <p>
 */

package com.dtech.claim.dto.request.validator;

import com.dtech.claim.enums.InsuranceClaimDocTypes;
import com.dtech.claim.validator.ValidEnum;
import com.dtech.claim.validator.ValidFileType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportingDocumentValidatorDTO {
    @NotBlank(message = "File type is required.")
    @ValidEnum(enumClass = InsuranceClaimDocTypes.class, message = "Invalid file type.")
    private String type;
    @NotBlank(message = "File is required.")
    private String file;
    @NotBlank(message = "File type is required.")
    @ValidFileType(message = "Only PNG, JPEG, JPG, and PDF file types are allowed.")
    private String fileType;
    @NotBlank(message = "File name is required.")
    private String fileName;
}
