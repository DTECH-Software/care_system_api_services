
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
import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.repository.ApplicationUserRepository;
import com.dtech.claim.repository.DeathClaimRequestRepository;
import com.dtech.claim.repository.InsuranceClaimsAccountBalanceRepository;
import com.dtech.claim.repository.InsuranceClaimsRequestRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
    private InsuranceClaimsRequestRepository insuranceClaimsRequestRepository;

    @Autowired
    private DeathClaimRequestRepository deathClaimRequestRepository;
    @Autowired
    private InsuranceClaimsAccountBalanceRepository insuranceClaimsAccountBalanceRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> dashboardSummary(DashboardSummaryDTO dashboardSummaryDTO, Locale locale) {
        try {
            log.info("Get dashboard summary {}", dashboardSummaryDTO);
            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(dashboardSummaryDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                HashMap<String,Object> list = new HashMap<>();

                //get count insurance
                CountTypeResponseDTO summary = insuranceClaimsRequestRepository.
                        findSummary(dashboardSummaryDTO, user.getId());
                //get latest update insurance

                List<LatestUpdatedResponseDTO> approed = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.APPROVED.name());
                List<LatestUpdatedResponseDTO> rejected = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.REJECTED.name());
                List<LatestUpdatedResponseDTO> underReiew = insuranceClaimsRequestRepository.getLatestUpdatedRecordSummary(user.getId(), Workflow.UNDER_REVIEW.name());

                CountResponseDTO ff = new CountResponseDTO();

                ff.setApproved(approed);
                ff.setRejected(rejected);
                ff.setUnderReview(underReiew);
                ff.setCountDetails(summary);

                list.put("insurance", ff);

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
