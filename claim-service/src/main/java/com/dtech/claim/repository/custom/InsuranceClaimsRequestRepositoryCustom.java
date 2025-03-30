package com.dtech.claim.repository.custom;

import com.dtech.claim.dto.request.DashboardSummaryDTO;


public interface InsuranceClaimsRequestRepositoryCustom {
    Object[] getCounts(DashboardSummaryDTO dashboardSummaryDTO, Long userId);
}