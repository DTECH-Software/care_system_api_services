/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:56 AM
 * <p>
 */

package com.dtech.claim.service.impl;


import com.dtech.claim.dto.ClaimRequestIdGen;
import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.SupportingDocumentDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.enums.CommonParam;
import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.model.*;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.ClaimRequestService;
import com.dtech.claim.util.DateTimeUtil;
import com.dtech.claim.util.RequestIdGenUtil;
import com.dtech.claim.util.ResponseMessageUtil;
import com.dtech.claim.util.ResponseUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ClaimRequestServiceImpl implements ClaimRequestService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ClaimDependentsRepository claimDependentsRepository;

    @Autowired
    private final InsuranceDetailsRepository insuranceDetailsRepository;

    @Autowired
    private final InsuranceRepository insuranceRepository;

    @Autowired
    private final InsurancePeriodRepository insurancePeriodRepository;

    @Autowired
    private final TreatmentRepository treatmentRepository;

    @Autowired
    private final ClaimsAccountBalanceRepository claimsAccountBalanceRepository;

    @Autowired
    private final EntityManager entityManager;

    @Autowired
    private final DocumentRepository documentRepository;

    @Autowired
    private final InsuranceClaimsDetailsRepository insuranceClaimsDetailsRepository;

    @Autowired
    private final ClaimsRequestRepository claimsRequestRepository;

    @Autowired
    private final CommonParameterRepository commonParameterRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> claimRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claim request processing started {}", claimRequestDTO);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(claimRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {
                if (user.getInsurancePolicy() == null) {
                    log.info("User not eligible to claim request {}", claimRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1029, messageSource.getMessage(ResponseMessageUtil.USER_NOT_ELIGIBLE_TO_CLAIM_REQUEST, null, locale)));
                }
                return commonParameterRepository.findByCode(CommonParam.CLIM_REQUEST_PERIOD.name()).map((param) -> {
                            log.info("get - date from claim request {}", param);
                            Date minuesDate = DateTimeUtil.getMinuesDate(param.getValue());
                            if (claimRequestDTO.getToDate().before(minuesDate)) {
                                log.info("older than claim request {}", claimRequestDTO.getUsername());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1037, messageSource.getMessage(ResponseMessageUtil.OLDER_DATE_CLAIM_REQUEST, null, locale)));
                            }
                            return insuranceRepository.findByIdAndStatus(user.getInsurancePolicy().getId(), Status.ACTIVE).map((policy) -> {
                                return insurancePeriodRepository.findByYearAndStatus(String.valueOf(LocalDate.now().getYear()), Status.ACTIVE).map((period) -> {
                                    return treatmentRepository.findByTreatmentCode(claimRequestDTO.getTreatment()).map((treatment) -> {
                                        return insuranceDetailsRepository.findByInsurancePolicyAndInsurancePeriodAndTreatmentAndStatus(policy, period, treatment, Status.ACTIVE).map((insuranceDetails) -> {

                                            Optional<ClaimsDependents> claimsDependents = Optional.empty();

                                            if (!claimRequestDTO.getIsEmployee()) {
                                                log.info("Claim dependent found for request {}", true);
                                                claimsDependents = claimDependentsRepository.findByIdAndApplicationUserAndStatus(claimRequestDTO.getClaimsDependentId(), user, Workflow.ACTIVE);

                                                if (claimsDependents.isEmpty()) {
                                                    log.info("Claim dependent not found");
                                                    return ResponseEntity.ok().body(responseUtil.error(null, 1034, messageSource.getMessage(ResponseMessageUtil.CLAIM_DEPENDENT_NOT_FOUND, null, locale)));
                                                }
                                            }

                                            Optional<ClaimsAccountBalance> claimsAccountBalance = claimsAccountBalanceRepository.findByEmployeeAndTreatmentAndInsurancePeriod(user, treatment, period);

                                            //check available fund
                                            String message = checkFundLimits(insuranceDetails, claimRequestDTO, claimsAccountBalance.orElse(null));

                                            if (message != null && !message.isEmpty()) {
                                                log.info("validation filed {} ", message);
                                                return ResponseEntity.ok().body(responseUtil.error(null, 1035, message));
                                            }
                                            saveClaimRequest(claimRequestDTO, period, user, claimsDependents, treatment);
                                            updateAccountBalance(claimsAccountBalance.orElse(null), claimRequestDTO, treatment, insuranceDetails, user, period);
                                            return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.CLAIM_REQUEST_SUBMIT_SUCCESS, null, locale)));

                                        }).orElseGet(() -> {
                                            log.info("User insurance policy period treatment not found");
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1033, messageSource.getMessage(ResponseMessageUtil.POLICY_TREATMENT_PERIOD_NOT_FOUND_OR_INACTIVE, null, locale)));
                                        });
                                    }).orElseGet(() -> {
                                        log.info("User insurance treatment not found {} ", DateTimeUtil.getCurrentDateTime());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1032, messageSource.getMessage(ResponseMessageUtil.TREATMENT_NOT_FOUND, null, locale)));
                                    });
                                }).orElseGet(() -> {
                                    log.info("User insurance period not found {} ", DateTimeUtil.getCurrentDateTime());
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1031, messageSource.getMessage(ResponseMessageUtil.INSURANCE_PERIOD_NOT_FOUND, null, locale)));
                                });
                            }).orElseGet(() -> {
                                log.info("User insurance policy not found {} ", user.getInsurancePolicy().getId());
                                return ResponseEntity.ok().body(responseUtil.error(null, 1030, messageSource.getMessage(ResponseMessageUtil.INSURANCE_POLICY_NOT_FOUND, null, locale)));
                            });
                        })
                        .orElseGet(() -> {
                            log.info("User common param claim request {}", claimRequestDTO.getUsername());
                            return ResponseEntity.ok().body(responseUtil.error(null, 1036, messageSource.getMessage(ResponseMessageUtil.COMMON_PARAM_NOT_FOUND, null, locale)));

                        });

            }).orElseGet(() -> {
                log.info("User claim request user not found {} ", claimRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateAccountBalance(ClaimsAccountBalance claimsAccountBalance, ClaimRequestDTO claimRequestDTO, Treatment treatment, InsuranceDetails insuranceDetails, ApplicationUser applicationUser, InsurancePeriod insurancePeriod) {
        try {
            log.info("Claim request account balance update started {}", claimRequestDTO);

            if (claimsAccountBalance == null) {
                log.info("Claim request account balance is null");
                claimsAccountBalance = new ClaimsAccountBalance();
                claimsAccountBalance.setUtilizeAmount(claimRequestDTO.getRequestAmount());
                claimsAccountBalance.setAvailableBalance(insuranceDetails.getClaimLimit().subtract(claimRequestDTO.getRequestAmount()));
                claimsAccountBalance.setEmployee(applicationUser);
                claimsAccountBalance.setTreatment(treatment);
                claimsAccountBalance.setInsurancePeriod(insurancePeriod);

            } else {
                log.info("claim request account balance not null {}", claimRequestDTO);
                claimsAccountBalance.setUtilizeAmount(claimsAccountBalance.getUtilizeAmount().add(claimRequestDTO.getRequestAmount()));
                claimsAccountBalance.setAvailableBalance(claimsAccountBalance.getAvailableBalance().subtract(claimRequestDTO.getRequestAmount()));
            }
            claimsAccountBalanceRepository.saveAndFlush(claimsAccountBalance);
            log.info("claim request account balance update finished");
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


    @Transactional
    protected InsuranceClaimsDetails saveClaimRequestDetails(ClaimRequestDTO claimRequestDTO, Treatment treatment) {
        try {
            log.info("Claim request details save started {}", claimRequestDTO);
            InsuranceClaimsDetails insuranceClaimsDetails = new InsuranceClaimsDetails();
            insuranceClaimsDetails.setTreatment(treatment);
            insuranceClaimsDetails.setFromTreatmentDate(claimRequestDTO.getFromDate());
            insuranceClaimsDetails.setToTreatmentDate(claimRequestDTO.getToDate());
            insuranceClaimsDetails.setDisease(claimRequestDTO.getDisease());
            insuranceClaimsDetails.setDocuments(findClaimsDocument(claimRequestDTO.getDocuments()));
            return insuranceClaimsDetailsRepository.saveAndFlush(insuranceClaimsDetails);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected List<Document> findClaimsDocument(List<SupportingDocumentDTO> supportingDocumentDTO) {
        try {
            log.info("User claims request document save {} ", supportingDocumentDTO);

            return supportingDocumentDTO.stream().map(val -> documentRepository.findById(val.getId()).orElseThrow(() -> new RuntimeException("Document not found with id " + val.getId()))).collect(Collectors.toList());

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void saveClaimRequest(ClaimRequestDTO claimRequestDTO, InsurancePeriod insurancePeriod, ApplicationUser applicationUser,
                                    Optional<ClaimsDependents> claimsDependents, Treatment treatment) {
        try {
            log.info("Claim request save started {}", claimRequestDTO);

            InsuranceClaimsDetails insuranceClaimsDetails = saveClaimRequestDetails(claimRequestDTO, treatment);

            ClaimRequestIdGen claimRequestIdGen = ClaimRequestIdGen.builder().year(insurancePeriod.getYear()).company(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode()).staffCategory(applicationUser.getUserPersonalDetails().getUserCompanyDetails().getStaffTypes().getCode()).build();
            RequestIdGenUtil requestIdGenUtil = new RequestIdGenUtil();
            log.info("Generate request id {}", claimRequestIdGen);
            String claimRequestId = (String) requestIdGenUtil.generate(entityManager.unwrap(SharedSessionContractImplementor.class), claimRequestIdGen);
            log.info("after generate request id {}", claimRequestId);
            ClaimsRequest claimsRequest = new ClaimsRequest();
            claimsRequest.setRequestId(claimRequestId);
            claimsRequest.setRequestAmount(claimRequestDTO.getRequestAmount());
            claimsRequest.setRequestStatus(Workflow.UNDER_REVIEW);
            claimsRequest.setRemark(claimRequestDTO.getRemark());
            claimsRequest.setClaimsDependents(claimsDependents.orElse(null));
            claimsRequest.setEmployee(applicationUser);
            claimsRequest.setInsuranceClaimsDetails(insuranceClaimsDetails);
            claimsRequestRepository.saveAndFlush(claimsRequest);
            log.info("Complete save claim request id {}", claimRequestId);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected String checkFundLimits(InsuranceDetails insuranceDetails, ClaimRequestDTO claimRequestDTO, ClaimsAccountBalance claimsAccountBalance) {
        try {
            log.info("Check global credit limit");
            String message = "";
            if (claimsAccountBalance != null) {
                message = checkClaimLimitExceeded(claimsAccountBalance.getAvailableBalance(), claimRequestDTO.getRequestAmount(), "Claim limit exceeded", "val.request.account.balance.insufficient");
                if (message != null) {
                    return message;
                }
            } else {
                message = checkClaimLimitExceeded(insuranceDetails.getClaimLimit(), claimRequestDTO.getRequestAmount(), "Claim global limit exceeded", "val.request.credit.limit.exceed.to.global.limit");
                if (message != null) {
                    return message;
                }
            }
            return message;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private String checkClaimLimitExceeded(BigDecimal claimLimit, BigDecimal requestAmount, String logMessage, String message) {
        if (claimLimit.compareTo(requestAmount) < 0) {
            log.info(logMessage, claimLimit, requestAmount);
            return messageSource.getMessage(message, null, null);
        }
        return null;
    }
}
