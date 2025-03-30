/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 8:48 AM
 * <p>
 */

package com.dtech.claim.repository.custom;

import com.dtech.claim.dto.request.DashboardSummaryDTO;

public interface DeathClaimsRequestRepositoryCustom {
    Object[] getCounts(DashboardSummaryDTO dashboardSummaryDTO, Long userId);
}
