/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:09 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupportingDocumentValidatorDTO {
    @NotNull(message = "Document id is required.")
    private String id;
}
