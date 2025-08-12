
/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 7:46 AM
 * <p>
 */
 
package com.dtech.claim.service.impl;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.CountResponseDTO;
import com.dtech.claim.dto.response.CountTypeResponseDTO;
import com.dtech.claim.dto.response.LatestUpdatedResponseDTO;
import com.dtech.claim.enums.Facility;
import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.InsuranceYear;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.DashboardService;
import com.dtech.claim.util.ResponseMessageUtil;
import com.dtech.claim.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    private final InsurancePeriodRepository insurancePeriodRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> dashboardSummary(DashboardSummaryDTO dashboardSummaryDTO, Locale locale) {
        try {
            log.info("Get dashboard summary {}", dashboardSummaryDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(dashboardSummaryDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                HashMap<String,Object> list = new HashMap<>();

                /*Insurance*/
                log.info("insurance claims");
                List<LatestUpdatedResponseDTO> approved = null;
                List<LatestUpdatedResponseDTO> rejected = null;
                List<LatestUpdatedResponseDTO> underReview = null;
                CountResponseDTO insurance = null;
                CountResponseDTO death = null;

                if(user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.INSURANCE.name()) ||
                        user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.BOTH.name())){
                    CountTypeResponseDTO countOfInsurance = insuranceClaimsRequestRepository.
                            findSummary(dashboardSummaryDTO, user.getId(),user.getUserPersonalDetails().getUserCompanyDetails().getInsurancePolicy().getCode());
                    log.info("insurance claims counts success");
                    //get latest updated insurance
                     approved = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.APPROVED.name());
                     rejected = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.REJECTED.name());
                     underReview = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.UNDER_REVIEW.name());
                    log.info("insurance claims list success");
                     insurance = CountResponseDTO.builder()
                            .approved(approved)
                            .rejected(rejected)
                            .underReview(underReview)
                            .countDetails(countOfInsurance).build();
                    log.info("insurance claims set dto success");
                }

                if(user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.DEATH.name()) ||
                        user.getUserPersonalDetails().getUserCompanyDetails().getFacility().name().equals(Facility.BOTH.name())){
                    /*Death*/
                    log.info("death claims");
                    CountTypeResponseDTO countOfDeath = deathClaimRequestRepository.
                            findSummary(dashboardSummaryDTO, user.getId());
                    log.info("death claims counts success");
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
                Optional<InsuranceYear> activeYear = insurancePeriodRepository.
                        findByCodeAndStatus(String.valueOf(LocalDate.now().getYear()), Status.ACTIVE);

                //get latest updated death
                list.put("insurance", insurance);
                list.put("death", death);
                list.put("activeYear", activeYear.isPresent() ? activeYear.get().getCode() : 0);
                return ResponseEntity.ok().body(responseUtil.success((Object) list, messageSource.getMessage(ResponseMessageUtil.DASHBOARD_SUMMARY_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.info("Dashboard summary request user not found {} ", dashboardSummaryDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
