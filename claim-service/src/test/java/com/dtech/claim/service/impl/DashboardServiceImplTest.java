package com.dtech.claim.service.impl;

import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void shouldNotTreatLastPolicyDayAsBeforeTransfer() {
        InsuranceStaffCategoryPeriod period = policyPeriod("2026-08-13", "2027-08-12");

        assertFalse(DashboardServiceImpl.isPolicyPeriodEntirelyBeforeTransfer(
                period,
                java.util.Date.from(Instant.parse("2027-08-12T18:29:59Z"))
        ));
    }

    @Test
    void shouldSelectPolicyUntilEndOfLastDayButNotNextDay() {
        InsuranceStaffCategoryPeriod period = policyPeriod("2026-08-13", "2027-08-12");

        assertEquals(period, DashboardServiceImpl.selectCurrentPeriod(
                List.of(period), java.util.Date.from(Instant.parse("2027-08-12T18:29:59Z"))));
        assertNull(DashboardServiceImpl.selectCurrentPeriod(
                List.of(period), java.util.Date.from(Instant.parse("2027-08-12T18:30:00Z"))));
    }

    @Test
    void shouldIncludeCurrentAndPreviousPeriodIdsForTransferYear() {
        InsuranceStaffCategoryPeriod currentPeriod = policyPeriod(14L, "2026-07-01", "2027-06-30");
        InsuranceStaffCategoryPeriod previousOverlappingPeriod = policyPeriod(4L, "2026-07-01", "2027-06-30");
        InsuranceStaffCategoryPeriod previousCompletedPeriod = policyPeriod(3L, "2025-07-01", "2026-06-30");

        assertEquals(
                List.of(14L, 4L),
                DashboardServiceImpl.collectDashboardPolicyPeriodIds(
                        currentPeriod,
                        Date.valueOf("2026-08-01"),
                        List.of(previousCompletedPeriod, previousOverlappingPeriod)
                )
        );
    }

    @Test
    void shouldUseOnlySelectedPeriodOutsideTransferYear() {
        InsuranceStaffCategoryPeriod currentPeriod = policyPeriod(14L, "2026-07-01", "2027-06-30");
        InsuranceStaffCategoryPeriod previousPeriod = policyPeriod(4L, "2025-07-01", "2026-06-30");

        assertEquals(
                List.of(14L),
                DashboardServiceImpl.collectDashboardPolicyPeriodIds(
                        currentPeriod,
                        Date.valueOf("2028-01-01"),
                        List.of(previousPeriod)
                )
        );
    }

    private InsuranceStaffCategoryPeriod policyPeriod(String fromDate, String toDate) {
        return policyPeriod(null, fromDate, toDate);
    }

    private InsuranceStaffCategoryPeriod policyPeriod(Long id, String fromDate, String toDate) {
        InsuranceStaffCategoryPeriod period = new InsuranceStaffCategoryPeriod();
        period.setId(id);
        period.setFromDate(Date.valueOf(fromDate));
        period.setToDate(Date.valueOf(toDate));
        return period;
    }
}
