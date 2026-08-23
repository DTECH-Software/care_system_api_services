/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 2:34 PM
 * <p>
 */

package com.dtech.document.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class DocumentDownloadRequestValidatorDTO{
    @NotNull(message = "Id is required.")
    private Long id;
    private boolean state = false;
}
