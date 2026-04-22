package com.dtech.claim.service.impl;

import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.TreatmentType;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.InsurancePolicy;
import com.dtech.claim.model.InsuranceStaffCategoryPeriod;
import com.dtech.claim.repository.ApplicationUserRepository;
import com.dtech.claim.repository.InsuranceClaimsRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class RejoinCarryForwardService {

    private final ApplicationUserRepository applicationUserRepository;
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Transactional(readOnly = true)
    public BigDecimal getApprovedAmountByTreatment(ApplicationUser currentUser,
                                                   String treatmentCode,
                                                   Long insurancePeriodId,
                                                   InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumApprovedAmountByTreatmentForPeriods(user, treatmentCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getApprovedAmountByTreatmentCategory(ApplicationUser currentUser,
                                                           String treatmentCode,
                                                           String categoryCode,
                                                           Long insurancePeriodId,
                                                           InsuranceStaffCategoryPeriod previousPeriod) {
        return resolveRelevantUsers(currentUser).stream()
                .map(user -> sumApprovedAmountByCategoryForPeriods(user, treatmentCode, categoryCode, insurancePeriodId, previousPeriod))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Date resolveEffectivePermanentDateForLimit(ApplicationUser currentUser) {
        // Rejoin carry-forward should affect utilized amounts only.
        // Entitlement quarter/fund-limit must come from the current profile's inclusion date.
        if (currentUser == null
                || currentUser.getUserPersonalDetails() == null
                || currentUser.getUserPersonalDetails().getUserCompanyDetails() == null) {
            return null;
        }
        return currentUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();
    }

    private List<ApplicationUser> resolveRelevantUsers(ApplicationUser currentUser) {
        List<ApplicationUser> users = new ArrayList<>();
        if (currentUser == null) {
            return users;
        }

        users.add(currentUser);
        findPreviousInactiveUser(currentUser)
                .filter(previousUser -> isEligibleForCarryForward(currentUser, previousUser))
                .filter(previousUser -> !previousUser.getId().equals(currentUser.getId()))
                .ifPresent(users::add);
        return users;
    }

    private Optional<ApplicationUser> findPreviousInactiveUser(ApplicationUser currentUser) {
        if (currentUser == null
                || currentUser.getId() == null
                || currentUser.getUserPersonalDetails() == null) {
            return Optional.empty();
        }

        String nic = normalize(currentUser.getUserPersonalDetails().getNic());
        if (nic == null) {
            return Optional.empty();
        }

        return applicationUserRepository
                .findTopByUserPersonalDetails_NicIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNotOrderByIdDesc(
                        nic,
                        Status.INACTIVE,
                        currentUser.getId()
                );
    }

    private boolean isEligibleForCarryForward(ApplicationUser currentUser, ApplicationUser previousUser) {
        String currentEpf = currentUser.getUserPersonalDetails() != null
                ? normalize(currentUser.getUserPersonalDetails().getEpfNo())
                : null;
        String previousEpf = previousUser.getUserPersonalDetails() != null
                ? normalize(previousUser.getUserPersonalDetails().getEpfNo())
                : null;

        if (currentEpf != null && currentEpf.equalsIgnoreCase(previousEpf)) {
            return false;
        }

        InsurancePolicy currentPolicy = currentUser.getUserPersonalDetails() != null
                && currentUser.getUserPersonalDetails().getUserCompanyDetails() != null
                ? currentUser.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy()
                : null;
        InsurancePolicy previousPolicy = previousUser.getUserPersonalDetails() != null
                && previousUser.getUserPersonalDetails().getUserCompanyDetails() != null
                ? previousUser.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy()
                : null;

        String currentPolicyCode = currentPolicy != null ? normalize(currentPolicy.getCode()) : null;
        String previousPolicyCode = previousPolicy != null ? normalize(previousPolicy.getCode()) : null;
        boolean eligible = currentPolicyCode != null
                && currentPolicyCode.equalsIgnoreCase(previousPolicyCode);

        if (eligible) {
            log.info("Applying rejoin carry forward. currentUserId={}, previousUserId={}, policy={}",
                    currentUser.getId(), previousUser.getId(), currentPolicyCode);
        }
        return eligible;
    }

    private BigDecimal sumApprovedAmountByTreatmentForPeriods(ApplicationUser user,
                                                              String treatmentCode,
                                                              Long insurancePeriodId,
                                                              InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safeSum(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                user,
                treatmentCode,
                insurancePeriodId,
                java.util.List.of(Workflow.APPROVED)
        ));

        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safeSum(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                    user,
                    treatmentCode,
                    previousPeriodId,
                    java.util.List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private BigDecimal sumApprovedAmountByCategoryForPeriods(ApplicationUser user,
                                                             String treatmentCode,
                                                             String categoryCode,
                                                             Long insurancePeriodId,
                                                             InsuranceStaffCategoryPeriod previousPeriod) {
        BigDecimal total = safeSum(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                user,
                treatmentCode,
                categoryCode,
                insurancePeriodId,
                java.util.List.of(Workflow.APPROVED)
        ));

        Long previousPeriodId = shouldApplyPromotionCarryForward(treatmentCode) && previousPeriod != null
                ? previousPeriod.getId()
                : null;
        if (previousPeriodId != null && !previousPeriodId.equals(insurancePeriodId)) {
            total = total.add(safeSum(insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndTreatmentCategoryAndStatus(
                    user,
                    treatmentCode,
                    categoryCode,
                    previousPeriodId,
                    java.util.List.of(Workflow.APPROVED)
            )));
        }
        return total;
    }

    private BigDecimal safeSum(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean shouldApplyPromotionCarryForward(String treatmentCode) {
        return TreatmentType.OUTDOOR.name().equalsIgnoreCase(normalize(treatmentCode));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
