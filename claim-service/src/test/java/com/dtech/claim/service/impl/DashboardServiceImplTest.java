package com.dtech.claim.service.impl;

import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceImplTest {

    @Test
    void shouldUsePreviousPolicyForPeriodCompletedBeforeTransfer() {
        InsuranceStaffCategoryPeriod previousPeriod = policyPeriod("2025-07-01", "2026-06-30");

        assertTrue(DashboardServiceImpl.isPolicyPeriodEntirelyBeforeTransfer(
                previousPeriod,
                Date.valueOf("2026-08-01")
        ));
    }

    @Test
    void shouldUseCurrentPolicyWhenTransferOccursInsidePolicyPeriod() {
        InsuranceStaffCategoryPeriod transferYearPeriod = policyPeriod("2026-07-01", "2027-06-30");

        assertFalse(DashboardServiceImpl.isPolicyPeriodEntirelyBeforeTransfer(
                transferYearPeriod,
                Date.valueOf("2026-08-01")
        ));
    }

    @Test
    void shouldNotTreatPeriodStartingOnTransferDateAsPrevious() {
        InsuranceStaffCategoryPeriod currentPeriod = policyPeriod("2026-08-01", "2027-07-31");

        assertFalse(DashboardServiceImpl.isPolicyPeriodEntirelyBeforeTransfer(
                currentPeriod,
                Date.valueOf("2026-08-01")
        ));
    }

    private InsuranceStaffCategoryPeriod policyPeriod(String fromDate, String toDate) {
        InsuranceStaffCategoryPeriod period = new InsuranceStaffCategoryPeriod();
        period.setFromDate(Date.valueOf(fromDate));
        period.setToDate(Date.valueOf(toDate));
        return period;
    }
}
