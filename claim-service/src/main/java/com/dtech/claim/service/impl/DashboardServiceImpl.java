
/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 7:46 AM
 * <p>
 */

package com.dtech.claim.service.impl;

import com.dtech.claim.dto.AvailableInsuranceLimitDTO;
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

                    AmountResponseDTO indoor = insuranceClaimsRequestRepository.findSummaryByFacility(dashboardSummaryDTO, user.getId(),
                            user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode(), TreatmentType.INDOOR.name());

                    Optional<InsuranceDetailsLimit> insuranceDetailsLimits = insuranceDetailsLimitRepository
                            .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                    Status.ACTIVE,
                                    currentPeriodPolicy,
                                    TreatmentType.INDOOR.name());

                    if (insuranceDetailsLimits.isPresent()) {
                        try {
                            BigDecimal totalLimit = setLimitMap(insuranceDetailsLimits.get(), user);
                            BigDecimal remaing = totalLimit.subtract(indoor.getSumOfUtilizeAmount());
                            indoor.setTotalLimit(totalLimit);
                            indoor.setRemainingAmount(remaing);
                        } catch (ParseException e) {
                            log.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }


                    countOfInsurance.setIndoor(indoor);

                    AmountResponseDTO outdoor = insuranceClaimsRequestRepository.findSummaryByFacility(dashboardSummaryDTO, user.getId(),
                            user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode(), TreatmentType.OUTDOOR.name()
                    );

                    insuranceDetailsLimits = insuranceDetailsLimitRepository
                            .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                    Status.ACTIVE,
                                    currentPeriodPolicy,
                                    TreatmentType.OUTDOOR.name());

                    if (insuranceDetailsLimits.isPresent()) {
                        try {
                            BigDecimal totalLimit = setLimitMap(insuranceDetailsLimits.get(), user);
                            log.info("insurance claims counts success {}",totalLimit);
                            BigDecimal remaing = totalLimit.subtract(outdoor.getSumOfUtilizeAmount());
                            outdoor.setTotalLimit(totalLimit);
                            outdoor.setRemainingAmount(remaing);
                        } catch (ParseException e) {
                            log.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }

                    countOfInsurance.setOutdoor(outdoor);

                    AmountResponseDTO critical = insuranceClaimsRequestRepository.findSummaryByFacility(dashboardSummaryDTO, user.getId(),
                            user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode(), TreatmentType.CRIC.name());

                    insuranceDetailsLimits = insuranceDetailsLimitRepository
                            .findByInsurancePolicyAndStatusAndInsuranceStaffCategoryPeriodAndTreatment_TreatmentCode(
                                    user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy(),
                                    Status.ACTIVE,
                                    currentPeriodPolicy,
                                    TreatmentType.CRIC.name());

                    if (insuranceDetailsLimits.isPresent()) {
                        try {
                            BigDecimal totalLimit = setLimitMap(insuranceDetailsLimits.get(), user);
                            BigDecimal remaing = totalLimit.subtract(critical.getSumOfUtilizeAmount());
                            critical.setTotalLimit(totalLimit);
                            critical.setRemainingAmount(remaing);
                        } catch (ParseException e) {
                            log.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }

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
                Optional<InsuranceStaffCategoryPeriod> activeYear = insuranceStaffCategoryPeriodRepository.
                        findByStaffCategories_CodeAndStatus(user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode(), Status.ACTIVE);

                //get latest updated death
                list.put("insurance", insurance);
                list.put("death", death);
                list.put("activeYear", activeYear.isPresent() ? activeYear.get().getToDate() : 0);
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

}
