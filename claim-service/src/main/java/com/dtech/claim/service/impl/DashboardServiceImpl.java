
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
import com.dtech.claim.service.DashboardService;
import com.dtech.claim.util.DateTimeUtil;
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

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> dashboardSummary(DashboardSummaryDTO dashboardSummaryDTO, Locale locale) {
        try {
            log.info("Get dashboard summary {}", dashboardSummaryDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(dashboardSummaryDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                HashMap<String, Object> list = new HashMap<>();

                /*Insurance*/
                log.info("insurance claims");
                List<LatestUpdatedResponseDTO> approved = null;
                List<LatestUpdatedResponseDTO> rejected = null;
                List<LatestUpdatedResponseDTO> underReview = null;
                CountResponseDTO insurance = null;
                CountResponseDTO death = null;

                if (user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.INSURANCE.name()) ||
                        user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.BOTH.name())) {
                    CountTypeResponseDTO countOfInsurance = insuranceClaimsRequestRepository.
                            findSummary(dashboardSummaryDTO, user.getId(), user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode());
                    log.info("insurance claims counts success");

                    InsuranceStaffCategoryPeriod currentPeriodPolicy = insuranceStaffCategoryPeriodRepository
                            .findByDateWithinRange(DateTimeUtil.getCurrentDateTime(), user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode())
                            .filter(period -> period.getStatus() == Status.ACTIVE)
                            .orElse(null);

                    AmountResponseDTO indoor = buildAmountSummary(user, currentPeriodPolicy, TreatmentType.INDOOR.name());
                    countOfInsurance.setIndoor(indoor);

                    AmountResponseDTO outdoor = buildAmountSummary(user, currentPeriodPolicy, TreatmentType.OUTDOOR.name());
                    countOfInsurance.setOutdoor(outdoor);

                    AmountResponseDTO critical = buildAmountSummary(user, currentPeriodPolicy, TreatmentType.CRIC.name());

                    int i = 0;
                    if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("NS")) {
                        countOfInsurance.setCritical(null);
                        int requestApprovedCount = insuranceClaimsRequestRepository.findApprovedRequestCountByTreatment(dashboardSummaryDTO,
                                user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode(), TreatmentType.CRIC.name());
                        i = 4 - requestApprovedCount;
                        i = Math.max(i, 0);
                    } else if (user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode().equals("SNR")) {
                        int requestApprovedCount = insuranceClaimsRequestRepository.findApprovedRequestCountByTreatmentSNR(dashboardSummaryDTO, user.getId(),
                                user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode(), TreatmentType.CRIC.name());
                        i = 4 - requestApprovedCount;
                        i = Math.max(i, 0);
                        countOfInsurance.setCritical(critical);
                    } else {
                        countOfInsurance.setCritical(critical);
                    }

                    //get latest updated insurance
                    approved = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.APPROVED.name());
                    rejected = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.REJECTED.name());
                    underReview = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.UNDER_REVIEW.name());
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

                // get active period
                List<InsuranceStaffCategoryPeriod> activePeriods = insuranceStaffCategoryPeriodRepository
                        .findAllByStaffCategories_CodeAndStatus(
                                user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode(),
                                Status.ACTIVE);
                InsuranceStaffCategoryPeriod activeYear = resolvePeriodByYear(activePeriods, dashboardSummaryDTO.getYear());

                //get latest updated death
                list.put("insurance", insurance);
                list.put("death", death);
                list.put("activeYear", activeYear != null ? activeYear.getToDate() : 0);
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
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(channelRequestDTO.getUsername().trim(), Status.ACTIVE)
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();

                        String staffCode = user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode();
                        List<InsuranceStaffCategoryPeriod> periods = insuranceStaffCategoryPeriodRepository
                                .findAllByStaffCategories_CodeAndStatus(staffCode, Status.ACTIVE);

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

        Date now = DateTimeUtil.getCurrentDateTime();
        for (InsuranceStaffCategoryPeriod period : periods) {
            Date from = period.getFromDate();
            Date to = period.getToDate();
            if (from != null && to != null && !now.before(from) && !now.after(to)) {
                return period;
            }
        }

        return periods.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(InsuranceStaffCategoryPeriod::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
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

            BigDecimal totalFund = BigDecimal.ZERO;

            log.info("Claims limit {} ", "test");

            Date permentDateTime = applicationUser.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate();

            if (permentDateTime.after(DateTimeUtil.getCurrentDateTime())) {
                log.info("Pre Year");
                BigDecimal maxLimit = BigDecimal.valueOf(0.00);

                return insuranceDetailsLimit.getGlobalLimit();
                //  totalFund = totalFund.add(maxLimit);


            } else {

                log.info("Post Year");

                //   BigDecimal maxLimit = BigDecimal.valueOf(0.00);

                InsuranceQuarter treatmentQuarter = insuranceQuarterRepository.findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit,
                        TreatmentCategory.OTHER.name(), permentDateTime).orElse(null);

                if (treatmentQuarter != null) {
                    return treatmentQuarter.getQuarterLimit();
                } else {
                    return insuranceDetailsLimit.getGlobalLimit();
                }

                //  totalFund = totalFund.add(maxLimit);

            }

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
                        user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                        Status.ACTIVE,
                        currentPeriod,
                        treatmentCode);

        if (insuranceDetailsLimitOpt.isEmpty()) {
            return dto;
        }

        InsuranceDetailsLimit insuranceDetailsLimit = insuranceDetailsLimitOpt.get();
        BigDecimal sum = insuranceClaimsRequestRepository.getSumRequestAmountByEmployeeAndTreatmentAndStatus(
                user,
                treatmentCode,
                currentPeriod.getId(),
                List.of(Workflow.APPROVED)
        );
        sum = sum != null ? sum : BigDecimal.ZERO;

        InsuranceStaffCategoryPeriod prevPeriod = null;
        if (currentPeriod.getFromDate() != null && currentPeriod.getStaffCategories() != null) {
            prevPeriod = insuranceStaffCategoryPeriodRepository
                    .findFirstByFromDateLessThanOrderByFromDateDesc(currentPeriod.getFromDate())
                    .orElse(null);
        }
        if (prevPeriod != null
                && prevPeriod.getStaffCategories() != null
                && !prevPeriod.getStaffCategories().getCode()
                .equals(currentPeriod.getStaffCategories().getCode())) {
            BigDecimal prevSum = insuranceClaimsRequestRepository
                    .getSumApprovedAmountByEmployeeAndTreatmentAndStaffCategory(
                            user,
                            treatmentCode,
                            prevPeriod.getStaffCategories().getCode(),
                            List.of(Workflow.APPROVED));
            if (prevSum != null) {
                sum = sum.add(prevSum);
            }
        }

        Map<String, InsuranceQuarter> categoryQuarterMap = new HashMap<>();
        Date currentDate = DateTimeUtil.getCurrentDateTime();
        for (InsuranceQuarter quarter : insuranceDetailsLimit.getInsuranceQuarters()) {
            String category = quarter.getTreatmentCategory().getCode();
            if (categoryQuarterMap.containsKey(category)) {
                continue;
            }
            InsuranceQuarter currentQuarter = insuranceQuarterRepository
                    .findByDateWithinRangeAndCodeWithLimit(insuranceDetailsLimit, category, currentDate)
                    .orElse(null);
            categoryQuarterMap.put(category, currentQuarter != null ? currentQuarter : quarter);
        }

        BigDecimal maxFundLimit = categoryQuarterMap.values().stream()
                .map(q -> insuranceDetailsLimit.getIsQuarter()
                        ? (q.getQuarterLimit() != null ? q.getQuarterLimit() : insuranceDetailsLimit.getGlobalLimit())
                        : insuranceDetailsLimit.getGlobalLimit())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(insuranceDetailsLimit.getGlobalLimit());

        BigDecimal remaining = (maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO).subtract(sum);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        dto.setSumOfUtilizeAmount(sum);
        dto.setTotalLimit(maxFundLimit != null ? maxFundLimit : BigDecimal.ZERO);
        dto.setRemainingAmount(remaining);
        return dto;
    }
}
