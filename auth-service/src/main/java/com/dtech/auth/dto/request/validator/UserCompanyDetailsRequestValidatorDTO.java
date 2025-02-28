/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 10:46 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsRequestValidatorDTO {
    @NotNull(message = "Company type is required.")
    private SimpleBaseValidatorDTO companyTypes;
    @NotNull(message = "Staff category is required.")
    private SimpleBaseValidatorDTO staffCategories;
    @NotNull(message = "Staff type is required.")
    private SimpleBaseValidatorDTO staffTypes;
    @NotNull(message = "Permanent date is required.")
    private Date permanentDate;
}
