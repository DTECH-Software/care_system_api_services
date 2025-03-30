/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 9:07 AM
 * <p>
 */

package com.dtech.claim.dto.request.validator;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DashboardSummaryValidatorDTO extends ChannelRequestValidatorDTO {

    @NotBlank(message = "Year is required.")
    private String year;
    private String month;
    private String claimDependentId;
    private String relationCategory;
    private String treatmentType;
}
