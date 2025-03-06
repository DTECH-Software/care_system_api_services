/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:56 AM
 * <p>
 */

package com.dtech.claim.service.impl;


import com.dtech.claim.dto.ClaimRequestIdGen;
import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.MessageRequestDTO;
import com.dtech.claim.dto.request.OtpRequestDTO;
import com.dtech.claim.dto.request.SupportingDocumentDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.MessageResponseDTO;
import com.dtech.claim.enums.CommonParam;
import com.dtech.claim.enums.NotificationsType;
import com.dtech.claim.enums.Status;
import com.dtech.claim.enums.Workflow;
import com.dtech.claim.feign.MessageFeignClient;
import com.dtech.claim.model.*;
import com.dtech.claim.repository.*;
import com.dtech.claim.service.ClaimRequestService;
import com.dtech.claim.util.*;
import com.google.gson.Gson;
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
import java.util.*;
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

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Autowired
    private final Gson gson;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> claimRequest(ClaimRequestDTO claimRequestDTO, Locale locale) {
        try {
            log.info("Claim request processing started {}", claimRequestDTO);

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(claimRequestDTO.getUsername().trim(), Status.ACTIVE).map((user) -> {

                if (user.getApplicationOtpSession() != null) {
                    log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                    if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime()) &&
                            user.getApplicationOtpSession().getOtp().equals(claimRequestDTO.getOtp()) && !user.getApplicationOtpSession().isValidated()) {
                        log.info("Otp request valid {} ", user.getApplicationOtpSession());
                        updateApplicationUserOtpData(user, user.getApplicationOtpSession());

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
                    }

                    log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                }
                log.info("Otp request otp session not found {} ", claimRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));

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
    protected void updateApplicationUserOtpData(ApplicationUser applicationUser, ApplicationOtpSession applicationOtpSession) {
        log.info("Update otp validation request otp records");
        applicationUser.setOtpAttemptCount(0);
        applicationOtpSession.setValidated(true);
        applicationUserRepository.saveAndFlush(applicationUser);
        applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> claimRequestOtp(OtpRequestDTO otpRequestDTO, Locale locale) {
        try {
            log.info("User claim request otp {}", otpRequestDTO.getUsername());
            String username = otpRequestDTO.getUsername().trim();

            return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE).map(user ->
                    applicationPasswordPolicyRepository.findPasswordPolicy().map((policy) -> {

                        if (user.getOtpAttemptCount() > policy.getOtpExceedCount()) {
                            log.info("Claim request OTP request attempt exceed {} , {}", user.getOtpAttemptCount()
                                    , policy.getAttemptExceedCount());
                            long minutes = DateTimeUtil.getMinutes(DateTimeUtil.getYyyyMMddHHMmSsTimeFormatter(DateTimeUtil.getSeconds(user.getOtpAttemptResetTime(), 2700)));
                            return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_EXCEED, new Object[]{minutes}, locale)));
                        } else if (user.getOtpAttemptCount() > 0) {
                            log.info("Claim request request otp session {}", user.getApplicationOtpSession());
                            Optional<ApplicationOtpSession> applicationOtpSession = applicationOtpSessionRepository.
                                    findById(user.getApplicationOtpSession() != null ? user.getApplicationOtpSession().getId() : 0);

                            if (applicationOtpSession.isPresent()) {
                                log.info("Claim request request otp session {}", applicationOtpSession.get());
                                if (DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime())) {
                                    log.info("Claim request request otp session valid this moment {}", DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(), 60));
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S, null, locale)));
                                }
                                log.info("Claim request send otp session attempt exceed greater than 0 {}", user);
                            } else {
                                log.info("Claim request otp session not found {}", applicationOtpSession);
                                return ResponseEntity.ok().body(responseUtil.error(null, 1011, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SESSION_NOT_FOUND, null, locale)));
                            }
                        }
                        log.info("Profile update send otp session send message {}", user);
                        return sendMessage(user, user.getPrimaryMobile(), locale, policy.getOtpExceedCount() - user.getOtpAttemptCount());
                    }).orElseGet(() -> {
                        log.info("Profile update request policy not found for username {} ", username);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
                    })).orElseGet(() -> {
                log.info("Profile update request user not found for username {} ", otpRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1009, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ResponseEntity<ApiResponse<Object>> sendMessage(ApplicationUser applicationUser, String mobileNo, Locale locale, int otpExceedCount) {
        try {
            log.info("Processing profile update request gen otp {} ", applicationUser.getUsername());
            String otp = RandomGeneratorUtil.getRandom6DigitNumber();
            log.info("Generate otp {} ", otp);
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(otp);
            messageRequestDTO.setMobileNo(mobileNo);
            messageRequestDTO.setType(NotificationsType.OTP.name());
            log.info("Before calling message service {}", messageFeignClient);
            ResponseEntity<ApiResponse<Object>> messageResponse = messageFeignClient.sendMessage(messageRequestDTO);
            log.info("After response message service {}", messageResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(messageResponse);
            log.info("After message mapper response {}", objectApiResponse);
            MessageResponseDTO messageResponseDTO = gson.fromJson(gson.toJson(objectApiResponse), MessageResponseDTO.class);
            log.info("Otp send status {}", messageResponseDTO);
            ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO != null ? messageResponseDTO.getSuccess() : 0);
            updateApplicationUser(applicationUser, applicationOtpSession);
            log.info("Application OTP session updated successfully");
            if (objectApiResponse != null) {
                return ResponseEntity.ok().body(responseUtil.success(Map.of("otpRequestAttempt", otpExceedCount), messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SEND_SUCCESS, null, locale)));
            }
            return ResponseEntity.ok().body(responseUtil.error(null, 1019, messageSource.getMessage(ResponseMessageUtil.OTP_SEND_FAILED, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApplicationOtpSession updateOtpSession(String otp, int state) {
        try {
            log.info("Claim request edt request gen otp application otp session update {} ", otp);
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setSuccess(state);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("Claim request request otp session update {} ", otpSession);
            return otpSession;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUser(ApplicationUser applicationUser, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Claim request otp request update application user {}", applicationUser);
            applicationUser.setApplicationOtpSession(applicationOtpSession);
            applicationUser.setOtpAttemptCount(applicationUser.getOtpAttemptCount() + 1);
            applicationUser.setOtpAttemptResetTime(DateTimeUtil.getCurrentDateTime());
            applicationUserRepository.saveAndFlush(applicationUser);
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
