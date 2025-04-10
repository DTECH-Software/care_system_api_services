/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 7:43 AM
 * <p>
 */

package com.dtech.claim.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class DashboardSummaryDTO extends ChannelRequestDTO{
    private String year;
    private String month;
    private String claimDependentId;
    private String relationCategory;
    private String treatmentType;
    private String insuranceMonthCategory;
}
