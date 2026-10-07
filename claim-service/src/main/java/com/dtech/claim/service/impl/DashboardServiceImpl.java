
/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 7:46 AM
 * <p>
 */

package com.dtech.claim.service.impl;

import com.dtech.claim.dto.AvailableInsuranceLimitDTO;
import com.dtech.claim.dto.SimpleBaseDTO;
import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.*;
import com.dtech.claim.enums.*;
import com.dtech.claim.enums.TreatmentCategory;
import com.dtech.claim.model.*;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.AssistedUserResolver;
import com.dtech.claim.service.DashboardService;
import com.dtech.claim.util.DateTimeUtil;
import com.dtech.claim.util.PolicyDateUtil;
import com.dtech.claim.util.ResponseMessageUtil;
import com.dtech.claim.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int NORMAL_STAFF_CRIC_MIN_PERMANENT_YEARS = 3;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private final DeathClaimRequestRepository deathClaimRequestRepository;

    @Autowired
    private final InsuranceStaffCategoryPeriodRepository insuranceStaffCategoryPeriodRepository;
    @Autowired
    private InsuranceDetailsLimitRepository insuranceDetailsLimitRepository;
    @Autowired
    private InsuranceQuarterRepository insuranceQuarterRepository;

    @Autowired
    private final RejoinCarryForwardService rejoinCarryForwardService;

    @Autowired
    private final AssistedUserResolver assistedUserResolver;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> dashboardSummary(DashboardSummaryDTO dashboardSummaryDTO, Locale locale) {
        try {
            normalizeDashboardFilters(dashboardSummaryDTO);
            log.info("Get dashboard summary {}", dashboardSummaryDTO);
            return assistedUserResolver.resolve(dashboardSummaryDTO).map((user) -> {

                HashMap<String, Object> list = new HashMap<>();
                List<InsuranceStaffCategoryPeriod> activePeriods = resolveApplicablePolicyPeriods(user);
                InsuranceStaffCategoryPeriod selectedPolicyPeriod = resolvePeriodByYear(activePeriods, dashboardSummaryDTO.getYear());
                Long selectedPolicyPeriodId = selectedPolicyPeriod != null ? selectedPolicyPeriod.getId() : null;
                List<Long> selectedPolicyPeriodIds = resolveDashboardPolicyPeriodIds(user, selectedPolicyPeriod);

                /*Insurance*/
                log.info("insurance claims");
                List<LatestUpdatedResponseDTO> approved = null;
                List<LatestUpdatedResponseDTO> rejected = null;
                List<LatestUpdatedResponseDTO> underReview = null;
                CountResponseDTO insurance = null;
                CountResponseDTO death = null;

                if (user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.INSURANCE.name()) ||
                        user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.BOTH.name())) {
                    CountTypeResponseDTO countOfInsurance = selectedPolicyPeriodIds.size() > 1
                            ? insuranceClaimsRequestRepository.findSummaryByPolicyPeriodIds(
                                    dashboardSummaryDTO, user.getId(), selectedPolicyPeriodIds)
                            : insuranceClaimsRequestRepository.findSummary(
                                    dashboardSummaryDTO, user.getId(), selectedPolicyPeriodId);
                    log.info("insurance claims counts success");

                    AmountResponseDTO indoor = buildAmountSummary(user, selectedPolicyPeriod, TreatmentType.INDOOR.name());
                    countOfInsurance.setIndoor(indoor);

                    AmountResponseDTO outdoor = buildAmountSummary(user, selectedPolicyPeriod, TreatmentType.OUTDOOR.name());
                    countOfInsurance.setOutdoor(outdoor);

                    AmountResponseDTO critical = buildAmountSummary(user, selectedPolicyPeriod, TreatmentType.CRIC.name());

                    int i = 0;
                    if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS")) {
                        int requestApprovedCount = insuranceClaimsRequestRepository.findApprovedRequestCountByTreatment(dashboardSummaryDTO,
                                selectedPolicyPeriodId, TreatmentType.CRIC.name());
                        i = 4 - requestApprovedCount;
                        i = Math.max(i, 0);
                        if (hasCompletedNormalStaffCricPermanentPeriod(user)) {
                            countOfInsurance.setCritical(critical);
                        } else {
                            countOfInsurance.setCritical(null);
                        }
                    } else if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("SNR")) {
                        int requestApprovedCount = insuranceClaimsRequestRepository.findApprovedRequestCountByTreatmentSNR(dashboardSummaryDTO, user.getId(),
                                selectedPolicyPeriodId, TreatmentType.CRIC.name());
                        i = 4 - requestApprovedCount;
                        i = Math.max(i, 0);
                        countOfInsurance.setCritical(critical);
                    } else {
                        countOfInsurance.setCritical(critical);
                    }

                    //get latest updated insurance
                    approved = getLatestUpdatedInsuranceClaims(
                            user.getId(), Workflow.APPROVED, selectedPolicyPeriodId, selectedPolicyPeriodIds);
                    rejected = getLatestUpdatedInsuranceClaims(
                            user.getId(), Workflow.REJECTED, selectedPolicyPeriodId, selectedPolicyPeriodIds);
                    underReview = getLatestUpdatedInsuranceClaims(
                            user.getId(), Workflow.UNDER_REVIEW, selectedPolicyPeriodId, selectedPolicyPeriodIds);
                    log.info("insurance claims list success");
                    insurance = CountResponseDTO.builder()
                            .approved(approved)
                            .rejected(rejected)
                            .underReview(underReview)
                            .countDetails(countOfInsurance)
                            .remaining(i).
                            build();
                    log.info("insurance claims set dto success");
                }

                if (user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.DEATH.name()) ||
                        user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.BOTH.name())) {
                    /*Death*/
                    log.info("death claims");
                    CountTypeResponseDTO countOfDeath = deathClaimRequestRepository.
                            findSummary(dashboardSummaryDTO, user.getId());
                    log.info("death claims counts success");

                    AmountResponseDTO deathUtilize = deathClaimRequestRepository.findSummaryByDeath(dashboardSummaryDTO, user.getId());
                    countOfDeath.setDeath(deathUtilize);
                    //get latest updated death
                    approved = deathClaimRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.APPROVED.name());
                    rejected = deathClaimRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.REJECTED.name());
                    underReview = deathClaimRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.UNDER_REVIEW.name());
                    log.info("death claims list success");
                    death = CountResponseDTO.builder()
                            .approved(approved)
                            .rejected(rejected)
                            .underReview(underReview)
                            .countDetails(countOfDeath).build();
                    log.info("death claims set dto success");
                }

                //get latest updated death
                list.put("insurance", insurance);
                list.put("death", death);
                list.put("activeYear", selectedPolicyPeriod != null ? selectedPolicyPeriod.getToDate() : 0);
                return ResponseEntity.ok().body(responseUtil.success((Object) list, messageSource.getMessage(ResponseMessageUtil.DASHBOARD_SUMMARY_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Dashboard summary request user not found {} ", dashboardSummaryDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> dashboardReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Dashboard reference data {}", channelRequestDTO);
            return assistedUserResolver.resolve(channelRequestDTO)
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();

                        List<InsuranceStaffCategoryPeriod> periods = resolveApplicablePolicyPeriods(user);

                        List<SimpleBaseDTO> years = periods.stream()
                                .map(InsuranceStaffCategoryPeriod::getFromDate)
                                .filter(Objects::nonNull)
                                .map(this::getYear)
                                .distinct()
                                .sorted()
                                .map(year -> new SimpleBaseDTO(String.valueOf(year), String.valueOf(year)))
                                .collect(Collectors.toList());

                        response.put("years", years);
                        return ResponseEntity.ok().body(responseUtil.success((Object) response,
                                messageSource.getMessage(ResponseMessageUtil.DASHBOARD_SUMMARY_SUCCESS, null, locale)));
                    })
                    .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1014,
                            messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale))));
        } catch (Exception e) {
            log.error("Failed to load dashboard reference data", e);
            throw e;
        }
    }

    private void normalizeDashboardFilters(DashboardSummaryDTO dashboardSummaryDTO) {
        if (dashboardSummaryDTO == null) {
            return;
        }
        dashboardSummaryDTO.setYear(normalizeText(dashboardSummaryDTO.getYear()));
        dashboardSummaryDTO.setMonth(normalizeOptionalFilter(dashboardSummaryDTO.getMonth()));
        dashboardSummaryDTO.setClaimDependentId(normalizeOptionalFilter(dashboardSummaryDTO.getClaimDependentId()));
        dashboardSummaryDTO.setRelationCategory(normalizeOptionalFilter(dashboardSummaryDTO.getRelationCategory()));
        dashboardSummaryDTO.setTreatmentType(normalizeOptionalFilter(dashboardSummaryDTO.getTreatmentType()));
        dashboardSummaryDTO.setInsuranceMonthCategory(normalizeOptionalFilter(dashboardSummaryDTO.getInsuranceMonthCategory()));
    }

    private String normalizeOptionalFilter(String value) {
        String normalized = normalizeText(value);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InsuranceStaffCategoryPeriod resolvePeriodByYear(List<InsuranceStaffCategoryPeriod> periods, String yearValue) {
        if (periods == null || periods.isEmpty()) {
            return null;
        }

        Integer year = null;
        if (yearValue != null && !yearValue.isBlank()) {
            try {
                year = Integer.parseInt(yearValue.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid year value '{}'", yearValue);
            }
        }

        if (year != null) {
            for (InsuranceStaffCategoryPeriod period : periods) {
                Date fromDate = period != null ? period.getFromDate() : null;
                if (fromDate != null && getYear(fromDate) == year) {
                    return period;
                }
            }
        }

        return selectCurrentPeriod(periods, PolicyDateUtil.todaySqlDate());
    }

    static InsuranceStaffCategoryPeriod selectCurrentPeriod(List<InsuranceStaffCategoryPeriod> periods, Date today) {
        if (periods == null) {
            return null;
        }
        for (InsuranceStaffCategoryPeriod period : periods) {
            if (period == null) {
                continue;
            }
            Date from = period.getFromDate();
            Date to = period.getToDate();
            if (PolicyDateUtil.contains(from, to, today)) {
                return period;
            }
        }

        return null;
    }

    private List<InsuranceStaffCategoryPeriod> resolveApplicablePolicyPeriods(ApplicationUser user) {
        if (user == null
                || user.getUserPersonalDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories() == null) {
            return List.of();
        }

        UserCompanyDetails companyDetails = user.getUserPersonalDetails().getUserCompanyDetails();
        Date transferDate = companyDetails.getTransferDate();
        List<InsuranceStaffCategoryPeriod> periods = new ArrayList<>(
                insuranceStaffCategoryPeriodRepository.findAllByStaffCategories_CodeAndStatus(
                        companyDetails.getStaffCategories().getCode(), Status.ACTIVE));

        if (transferDate != null && companyDetails.getPreviousStaffCategories() != null) {
            periods.removeIf(period -> isPolicyPeriodEntirelyBeforeTransfer(period, transferDate));
            periods.addAll(insuranceStaffCategoryPeriodRepository
                    .findAllByStaffCategories_CodeAndStatus(
                            companyDetails.getPreviousStaffCategories().getCode(), Status.ACTIVE)
                    .stream()
                    .filter(period -> isPolicyPeriodEntirelyBeforeTransfer(period, transferDate))
                    .toList());
        }

        return periods.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        InsuranceStaffCategoryPeriod::getId,
                        period -> period,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        InsuranceStaffCategoryPeriod::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Long> resolveDashboardPolicyPeriodIds(ApplicationUser user,
                                                       InsuranceStaffCategoryPeriod selectedPolicyPeriod) {
        if (selectedPolicyPeriod == null || selectedPolicyPeriod.getId() == null) {
            return List.of();
        }

        UserCompanyDetails companyDetails = user.getUserPersonalDetails().getUserCompanyDetails();
        Date transferDate = companyDetails.getTransferDate();
        List<InsuranceStaffCategoryPeriod> previousPeriods = List.of();
        if (companyDetails.getPreviousStaffCategories() != null
                && isDateWithinPolicyPeriod(transferDate, selectedPolicyPeriod)) {
            previousPeriods = insuranceStaffCategoryPeriodRepository
                    .findAllByStaffCategories_CodeAndStatus(
                            companyDetails.getPreviousStaffCategories().getCode(), Status.ACTIVE);
        }

        return collectDashboardPolicyPeriodIds(selectedPolicyPeriod, transferDate, previousPeriods);
    }

    static List<Long> collectDashboardPolicyPeriodIds(InsuranceStaffCategoryPeriod selectedPolicyPeriod,
                                                       Date transferDate,
                                                       Collection<InsuranceStaffCategoryPeriod> previousPeriods) {
        if (selectedPolicyPeriod == null || selectedPolicyPeriod.getId() == null) {
            return List.of();
        }

        LinkedHashSet<Long> periodIds = new LinkedHashSet<>();
        periodIds.add(selectedPolicyPeriod.getId());
        if (!isDateWithinPolicyPeriod(transferDate, selectedPolicyPeriod) || previousPeriods == null) {
            return List.copyOf(periodIds);
        }

        previousPeriods.stream()
                .filter(Objects::nonNull)
                .filter(period -> period.getId() != null)
                .filter(period -> isOverlappingPeriod(period, selectedPolicyPeriod))
                .map(InsuranceStaffCategoryPeriod::getId)
                .forEach(periodIds::add);
        return List.copyOf(periodIds);
    }

    private static boolean isDateWithinPolicyPeriod(Date date,
                                                    InsuranceStaffCategoryPeriod period) {
        return date != null
                && period != null
                && period.getFromDate() != null
                && period.getToDate() != null
                && PolicyDateUtil.contains(period.getFromDate(), period.getToDate(), date);
    }

    private List<LatestUpdatedResponseDTO> getLatestUpdatedInsuranceClaims(
            Long userId,
            Workflow requestStatus,
            Long selectedPolicyPeriodId,
            List<Long> selectedPolicyPeriodIds) {
        if (selectedPolicyPeriodIds.size() > 1) {
            return insuranceClaimsRequestRepository.getLatestUpdatedRecordSummaryByPolicyPeriodIds(
                    userId, requestStatus.name(), selectedPolicyPeriodIds);
        }
        return insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(
                userId, requestStatus.name(), selectedPolicyPeriodId);
    }

    private int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }


    @Transactional(readOnly = true)
    public BigDecimal setLimitMap(InsuranceDetailsLimit insuranceDetailsLimit,
                                  ApplicationUser applicationUser) throws ParseException {

        try {
            log.info("Insurance ref {}", insuranceDetailsLimit.getId());

            log.info("Claims limit {} ", "test");

            Date permentDateTime = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(applicationUser);
            Date quarterLookupDate = permentDateTime != null ? permentDateTime : DateTimeUtil.getCurrentDateTime();
            InsuranceQuarter treatmentQuarter = resolveApplicableQuarter(insuranceDetailsLimit,
                    TreatmentCategory.OTHER.name(),
                    quarterLookupDate);

            if (treatmentQuarter != null) {
                return treatmentQuarter.getQuarterLimit();
            }
            if (insuranceDetailsLimit.getIsQuarter()) {
                return BigDecimal.ZERO;
            }
            return insuranceDetailsLimit.getGlobalLimit();

        } catch (Exception e) {
            log.error("Error calculating insurance limits", e);
            throw e;
        }
    }

    private AmountResponseDTO buildAmountSummary(ApplicationUser user,
                                                 InsuranceStaffCategoryPeriod currentPeriod,
                                                 String treatmentCode) {
        AmountResponseDTO dto = new AmountResponseDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        if (user == null || currentPeriod == null) {
            return dto;
        }

        Optional<InsuranceDetailsLimit> insuranceDetailsLimitOpt = insuranceDetailsLimitRepository
                .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                        resolveInsurancePolicyForPeriod(
                                user.getUserPersonalDetails().getUserCompanyDetails(), currentPeriod),
                        Status.ACTIVE,
                        currentPeriod,
                        treatmentCode);

        if (insuranceDetailsLimitOpt.isEmpty()) {
            log.warn("Dashboard exact policy limit not found. Falling back by staff period. user={}, policy={}, periodId={}, treatment={}",
                    user.getUsername(),
                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy() != null
                            ? user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode()
                            : null,
                    currentPeriod.getId(),
                    treatmentCode);
            insuranceDetailsLimitOpt = insuranceDetailsLimitRepository
                    .findFirstByStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                            Status.ACTIVE,
                            currentPeriod,
                            treatmentCode);
        }

        if (insuranceDetailsLimitOpt.isEmpty()) {
            return dto;
        }

        InsuranceDetailsLimit insuranceDetailsLimit = insuranceDetailsLimitOpt.get();
        Date previousPermanentDate = user.getUserPersonalDetails()
                .getUserCompanyDetails()
                .getPreviousPermanentDate();
        Date changeDate = previousPermanentDate != null
                ? user.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate()
                : null;
        InsuranceStaffCategoryPeriod prevPeriod = resolvePreviousPeriodFromClaimHistory(
                user,
                treatmentCode,
                currentPeriod);
        if (prevPeriod == null
                && changeDate != null
                && currentPeriod.getStaffCategories() != null) {
            prevPeriod = insuranceStaffCategoryPeriodRepository
                    .findByDateWithinRangeAnyStaff(PolicyDateUtil.toSqlDate(changeDate))
                    .stream()
                    .filter(p -> p.getStaffCategories() != null)
                    .filter(p -> !p.getStaffCategories().getCode()
                            .equals(currentPeriod.getStaffCategories().getCode()))
                    .findFirst()
                    .orElse(null);
        }
        if (prevPeriod != null) {
            log.info("Dashboard carry-forward also considers previous period {}", prevPeriod.getId());
        }
        final InsuranceStaffCategoryPeriod finalPrevPeriod = prevPeriod;

        BigDecimal directTreatmentApprovedSum = rejoinCarryForwardService.getApprovedAmountByTreatment(
                user,
                treatmentCode,
                currentPeriod.getId(),
                finalPrevPeriod
        );
        BigDecimal sum = directTreatmentApprovedSum;

        Map<String, InsuranceQuarter> categoryQuarterMap = new HashMap<>();
        Date permanentDate = rejoinCarryForwardService.resolveEffectivePermanentDateForLimit(user);
        Date quarterLookupDate = permanentDate != null ? permanentDate : DateTimeUtil.getCurrentDateTime();
        for (InsuranceQuarter quarter : insuranceDetailsLimit.getInsuranceQuarters()) {
            String category = quarter.getTreatmentCategory().getCode();
            if (categoryQuarterMap.containsKey(category)) {
                continue;
            }
            InsuranceQuarter matchingQuarter = resolveApplicableQuarter(insuranceDetailsLimit, category, quarterLookupDate);
            categoryQuarterMap.put(category, matchingQuarter);
        }

        BigDecimal categoryApprovedTotal = categoryQuarterMap.keySet().stream()
                .map(categoryCode -> rejoinCarryForwardService.getApprovedAmountByTreatmentCategory(
                        user,
                        treatmentCode,
                        categoryCode,
                        currentPeriod.getId(),
                        finalPrevPeriod
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (categoryApprovedTotal.compareTo(sum) > 0) {
            log.info("Dashboard using category aggregate for treatment {}. direct={}, categoryTotal={}",
                    treatmentCode, sum, categoryApprovedTotal);
            sum = categoryApprovedTotal;
        }

        BigDecimal maxFundLimit = categoryQuarterMap.values().stream()
                .map(q -> resolveCategoryFundLimit(insuranceDetailsLimit, q))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(insuranceDetailsLimit.getIsQuarter() ? BigDecimal.ZERO : insuranceDetailsLimit.getGlobalLimit());

        BigDecimal remaining = (maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO).subtract(sum);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        log.info("DASHBOARD_LIMIT_DEBUG user={}, treatment={}, periodId={}, prevPeriodId={}, directTreatmentApproved={}, categoryApprovedTotal={}, appliedTreatmentApproved={}, totalLimit={}, remaining={}",
                user.getUsername(),
                treatmentCode,
                currentPeriod.getId(),
                finalPrevPeriod != null ? finalPrevPeriod.getId() : null,
                directTreatmentApprovedSum,
                categoryApprovedTotal,
                sum,
                maxFundLimit,
                remaining);

        dto.setSumOfUtilizeAmount(sum);
        dto.setTotalLimit(maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO);
        dto.setRemainingAmount(remaining);
        return dto;
    }

    private InsurancePolicy resolveInsurancePolicyForPeriod(UserCompanyDetails companyDetails,
                                                             InsuranceStaffCategoryPeriod period) {
        if (companyDetails.getTransferDate() != null
                && isPolicyPeriodEntirelyBeforeTransfer(period, companyDetails.getTransferDate())
                && companyDetails.getPreviousInsurancePolicy() != null) {
            return companyDetails.getPreviousInsurancePolicy();
        }
        return companyDetails.getInsurancePolicy();
    }

    static boolean isPolicyPeriodEntirelyBeforeTransfer(InsuranceStaffCategoryPeriod period,
                                                         Date transferDate) {
        return period != null
                && period.getToDate() != null
                && transferDate != null
                && PolicyDateUtil.isBefore(period.getToDate(), transferDate);
    }

    private InsuranceStaffCategoryPeriod resolvePreviousPeriodFromClaimHistory(ApplicationUser applicationUser,
                                                                              String treatmentCode,
                                                                              InsuranceStaffCategoryPeriod currentPeriod) {
        if (applicationUser == null
                || currentPeriod == null
                || currentPeriod.getStaffCategories() == null
                || treatmentCode == null) {
            return null;
        }

        String currentStaffCode = currentPeriod.getStaffCategories().getCode();
        return insuranceClaimsRequestRepository
                .findAllByEmployeeAndRequestStatusIn(applicationUser, List.of(Workflow.APPROVED))
                .stream()
                .filter(claim -> claim.getInsuranceClaimsDetails() != null)
                .filter(claim -> claim.getInsuranceClaimsDetails().getTreatment() != null)
                .filter(claim -> treatmentCode.equalsIgnoreCase(
                        claim.getInsuranceClaimsDetails().getTreatment().getTreatmentCode()))
                .map(this::resolveClaimPeriod)
                .filter(Objects::nonNull)
                .filter(claimPeriod -> claimPeriod.getId() != null && currentPeriod.getId() != null)
                .filter(claimPeriod -> !claimPeriod.getId().equals(currentPeriod.getId()))
                .filter(claimPeriod -> claimPeriod.getStaffCategories() != null)
                .filter(claimPeriod -> !currentStaffCode.equals(claimPeriod.getStaffCategories().getCode()))
                .filter(claimPeriod -> isOverlappingPeriod(claimPeriod, currentPeriod))
                .max(Comparator.comparing(InsuranceStaffCategoryPeriod::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private InsuranceStaffCategoryPeriod resolveClaimPeriod(InsuranceClaimsRequest claim) {
        if (claim.getInsuranceDetailsLimit() != null
                && claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod() != null) {
            return claim.getInsuranceDetailsLimit().getInsuranceStaffCategoryPeriod();
        }
        if (claim.getInsuranceClaimsDetails() != null) {
            return claim.getInsuranceClaimsDetails().getInsuranceStaffCategoryPeriod();
        }
        return null;
    }

    private static boolean isOverlappingPeriod(InsuranceStaffCategoryPeriod candidate,
                                               InsuranceStaffCategoryPeriod currentPeriod) {
        if (candidate.getFromDate() == null || candidate.getToDate() == null
                || currentPeriod.getFromDate() == null || currentPeriod.getToDate() == null) {
            return true;
        }
        return PolicyDateUtil.overlaps(candidate.getFromDate(), candidate.getToDate(),
                currentPeriod.getFromDate(), currentPeriod.getToDate());
    }

    private InsuranceQuarter resolveApplicableQuarter(InsuranceDetailsLimit insuranceDetailsLimit,
                                                      String categoryCode,
                                                      Date lookupDate) {
        InsuranceQuarter matchingQuarter = insuranceQuarterRepository
                .findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, categoryCode, PolicyDateUtil.toSqlDate(lookupDate))
                .stream()
                .findFirst()
                .orElse(null);
        if (matchingQuarter != null) {
            return matchingQuarter;
        }

        InsuranceQuarter firstQuarter = insuranceQuarterRepository
                .findFirstByInsuranceDetailsLimitAndTreatmentCategory_CodeOrderByFromDateAsc(
                        insuranceDetailsLimit,
                        categoryCode
                ).orElse(null);
        if (firstQuarter == null || lookupDate == null || firstQuarter.getFromDate() == null) {
            return null;
        }

        return PolicyDateUtil.isBefore(lookupDate, firstQuarter.getFromDate()) ? firstQuarter : null;
    }

    private BigDecimal resolveCategoryFundLimit(InsuranceDetailsLimit insuranceDetailsLimit,
                                                InsuranceQuarter insuranceQuarter) {
        if (!insuranceDetailsLimit.getIsQuarter()) {
            return insuranceDetailsLimit.getGlobalLimit();
        }
        return insuranceQuarter != null ? insuranceQuarter.getQuarterLimit() : null;
    }

    private boolean hasCompletedNormalStaffCricPermanentPeriod(ApplicationUser user) {
        Date permanentDate = resolvePermanentDateForCricEligibility(user);
        if (permanentDate == null) {
            return false;
        }
        Calendar eligibleDate = Calendar.getInstance();
        eligibleDate.setTime(permanentDate);
        eligibleDate.add(Calendar.YEAR, NORMAL_STAFF_CRIC_MIN_PERMANENT_YEARS);
        return !DateTimeUtil.getCurrentDateTime().before(eligibleDate.getTime());
    }

    private Date resolvePermanentDateForCricEligibility(ApplicationUser user) {
        if (user == null
                || user.getUserPersonalDetails() == null
                || user.getUserPersonalDetails().getUserCompanyDetails() == null) {
            return null;
        }
        UserCompanyDetails companyDetails = user.getUserPersonalDetails().getUserCompanyDetails();
        return companyDetails.getPreviousPermanentDate() != null
                ? companyDetails.getPreviousPermanentDate()
                : companyDetails.getPermanentDate();
    }
}
