/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 10:32 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.dtech.auth.enums.DocType;
import com.dtech.auth.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileImageUpdateRequestValidatorDTO extends ChannelRequestValidatorDTO{
    @NotBlank(message = "Image type is required.")
    @ValidEnum(enumClass = DocType.class, message = "Invalid image type.")
    private String type;
    @NotBlank(message = "File is required.")
    private String file;
}
