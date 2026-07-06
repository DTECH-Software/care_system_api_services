/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 11:57 AM
 * <p>
 */

package com.dtech.message.service.impl;

import com.dtech.message.dto.request.MessageRequestDTO;
import com.dtech.message.dto.request.OtpRequestDTO;
import com.dtech.message.dto.request.OtpValidationDTO;
import com.dtech.message.dto.response.ApiResponse;
import com.dtech.message.dto.response.MessageResponseDTO;
import com.dtech.message.dto.response.PolicyResponseDTO;
import com.dtech.message.enums.Messages;
import com.dtech.message.enums.MessageType;
import com.dtech.message.enums.Status;
import com.dtech.message.model.ApplicationOtpSession;
import com.dtech.message.model.ApplicationUser;
import com.dtech.message.model.OnboardingVerifiedMobile;
import com.dtech.message.repository.*;
import com.dtech.message.service.OtpService;
import com.dtech.message.util.DateTimeUtil;
import com.dtech.message.util.RandomGeneratorUtil;
import com.dtech.message.util.ResponseMessageUtil;
import com.dtech.message.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final String DUMMY_MOBILE_PREFIX = "0000";

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final OnboardingVerifiedMobileRepository onboardingVerifiedMobileRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final SendMessageServiceImpl sendMessageService;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final ApplicationUsernamePolicyRepository applicationUsernamePolicyRepository;

    @Autowired
    private final Gson gson;

    @Override
    @ColumnTransformer
    public ResponseEntity<ApiResponse<Object>> otpRequest(OtpRequestDTO otpRequestDTO, Locale locale) {
        try {
            log.info("Otp request {} ", otpRequestDTO);
            if (otpRequestDTO.getMessage().equalsIgnoreCase(Messages.SIGNUP_OTP_REQUEST.name())) {
                log.info("Processing SignupOtpRequest {}", otpRequestDTO);
                return userPersonalDetailsRepository.findByEpfNoAndNicIgnoreCaseAndUserStatus(otpRequestDTO.getSignupOtp().getEpfNo().trim(), otpRequestDTO.getSignupOtp().getNic().trim(), Status.ACTIVE)
                        .map(userPersonalDetails -> applicationPasswordPolicyRepository.findPasswordPolicy()
                                .map(pw -> {

                                    //check user's mobile already in use
                                    boolean alreadyUser = applicationUserRepository.
                                            existsByPrimaryMobileAndUserPersonalDetails_UserStatus(
                                                    otpRequestDTO.getPrimaryMobile(), Status.ACTIVE);
                                    //check user's email already in use
                                    boolean existsEmail = applicationUserRepository
                                            .existsByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(
                                                    otpRequestDTO.getSignupOtp().getPrimaryEmail().trim(), Status.ACTIVE);

                                    if (alreadyUser) {
                                        log.info("Signup request mobile already in use {} ", otpRequestDTO.getPrimaryMobile());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1025, messageSource.getMessage(ResponseMessageUtil.PRIMARY_MOBILE_ALREADY_IN_USE, null, locale)));
                                    }else if(existsEmail){
                                        log.info("Signup exists email {}", otpRequestDTO.getSignupOtp().getPrimaryEmail());
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1026, messageSource.getMessage(ResponseMessageUtil.PRIMARY_EMAIL_ALREADY_IN_USE, null, locale)));
                                    } else if (pw.getOnboardingOtpHistory() > 0) {
                                        log.info("Signup otp request policy - {}", pw.getOnboardingOtpHistory());

                                        Sort sort = Sort.by(Sort.Order.desc("createdDate"));
                                        List<OnboardingVerifiedMobile> onboardingVerifiedMobiles = onboardingVerifiedMobileRepository
                                                .findByEpfNoAndNicEqualsIgnoreCaseAndMobileAndVerified(otpRequestDTO.getSignupOtp().getEpfNo(), otpRequestDTO.getSignupOtp().getNic(),
                                                        otpRequestDTO.getPrimaryMobile().trim(), true, sort);

                                        LocalDateTime localDateTime = LocalDateTime.now().minusDays(pw.getOnboardingOtpHistory());
                                        boolean history = onboardingVerifiedMobiles.stream().anyMatch((verifiedMobile) -> verifiedMobile.getCreatedDate().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(localDateTime));

                                        if (history) {
                                            log.info("Sign up otp already verified");
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1018, messageSource.getMessage(ResponseMessageUtil.OTP_ALREADY_VERIFIED, null, locale)));
                                        }
                                    }
                                    String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                                    log.info("Generate otp - onboarding verified {} ", otp);
                                    MessageResponseDTO messageResponseDTO = sendMessageService.sendToCustomer(new MessageRequestDTO(otpRequestDTO.getPrimaryMobile(), MessageType.OTP.name(), otp));
                                    log.info("Signup otp request success");
                                    ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO.isSuccess());
                                    updateOnboardingVerifiedMobile(otpRequestDTO, applicationOtpSession);

                                    if (messageResponseDTO.isSuccess()) {
                                        return ResponseEntity.ok().body(responseUtil.success(null, messageResponseDTO.getMessage()));
                                    }

                                    return ResponseEntity.ok().body(
                                            responseUtil.error(null, 1038,
                                                    messageResponseDTO.getMessage()));

                                })
                                .orElseGet(() -> {
                                    log.info("Signup otp request password policy not found {}", otpRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
                                }))
                        .orElseGet(() -> {
                            log.info("Signup otp request user not found {}", otpRequestDTO);
                            return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{otpRequestDTO.getPrimaryMobile()}, locale)));
                        });
            } else if (otpRequestDTO.getMessage().equalsIgnoreCase(Messages.RESET_PASSWORD_OTP_REQUEST.name())
                    || otpRequestDTO.getMessage().equalsIgnoreCase(Messages.CLAIM_REQUEST_OTP_REQUEST.name())
                    || otpRequestDTO.getMessage().equalsIgnoreCase(Messages.PROFILE_UPDATE_OTP_REQUEST.name())){

                log.info("Processing reset password request gen otp {} ", otpRequestDTO.getUsername());
                String username = otpRequestDTO.getUsername().trim();
                Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);

                if (optionalUser.isEmpty()) {
                    log.info("Reset password OTP request find by email {} ", username);
                    optionalUser = applicationUserRepository.findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);
                    otpRequestDTO.setUsername(optionalUser.map(ApplicationUser::getUsername).orElse(""));
                }

                return optionalUser.map(user -> applicationPasswordPolicyRepository.findPasswordPolicy().map((policy) -> {

                    if (user.getOtpAttemptCount() >= policy.getOtpExceedCount()) {
                        log.info("Reset password OTP request attempt exceed {} , {}", user.getOtpAttemptCount(), policy.getOtpExceedCount());
                        long minutes = DateTimeUtil.getMinutes(DateTimeUtil.getYyyyMMddHHMmSsTimeFormatter(DateTimeUtil.getSeconds(user.getOtpAttemptResetTime(), 2700)));
                        return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_EXCEED, new Object[]{minutes}, locale)));
                    } else if (user.getOtpAttemptCount() > 0) {
                        log.info("Reset password request otp session {}", user.getApplicationOtpSession());
                        Optional<ApplicationOtpSession> applicationOtpSession = applicationOtpSessionRepository.
                                findById(user.getApplicationOtpSession() != null ? user.getApplicationOtpSession().getId() : 0);

                        if (applicationOtpSession.isPresent()) {
                            log.info("Reset password request otp session {}", applicationOtpSession.get());
                            if (DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime())) {
                                log.info("Reset password request otp session valid this moment {}", DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(), 60));
                                return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S, null, locale)));
                            }
                            log.info("Rest password send otp session attempt exceed greater than 0 {}", user);
                        } else {
                            log.info("Reset password request otp session not found {}", applicationOtpSession);
                            return ResponseEntity.ok().body(responseUtil.error(null, 1011, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SESSION_NOT_FOUND, null, locale)));
                        }
                    }

                    log.info("Rest password send otp session send message {}", user);
                    String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                    log.info("Generate otp - reset verified {} ", otp);
                    String deliveryMobile = resolveOtpDeliveryMobile(otpRequestDTO, user);
                    if (deliveryMobile == null) {
                        return ResponseEntity.ok().body(responseUtil.error(null, 1038,
                                "This employee does not have a registered mobile number. Please enter an assisted mobile number."));
                    }
                    MessageResponseDTO messageResponseDTO = sendMessageService.sendToCustomer(new MessageRequestDTO(deliveryMobile, MessageType.OTP.name(), otp));
                    log.info("Reset otp request success");
                    ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO.isSuccess());
                    updateApplicationUser(user, applicationOtpSession);
                    if (messageResponseDTO.isSuccess()) {
                        return ResponseEntity.ok().body(responseUtil.success((Object) Map.of("otpRequestAttempt",Math.max(0,policy.getOtpExceedCount() - user.getOtpAttemptCount())), messageResponseDTO.getMessage()));
                    }
                    return ResponseEntity.ok().body(
                            responseUtil.error(null, 1038,
                                    messageResponseDTO.getMessage()));
                }).orElseGet(() -> {
                    log.info("Password reset request policy not found for username {} ", username);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
                })).orElseGet(() -> {
                    log.info("Password otp reset password request user not found for username {} ", otpRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1009, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
                });
            }

            return null;

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUser(ApplicationUser applicationUser, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Rest password opt request update application user {}", applicationUser);
            applicationUser.setApplicationOtpSession(applicationOtpSession);
            applicationUser.setOtpAttemptCount(applicationUser.getOtpAttemptCount() + 1);
            applicationUser.setOtpAttemptResetTime(DateTimeUtil.getCurrentDateTime());
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private String resolveOtpDeliveryMobile(OtpRequestDTO otpRequestDTO, ApplicationUser user) {
        String userMobile = user != null ? user.getPrimaryMobile() : null;
        if (isAssistedClaimOtpRequest(otpRequestDTO) && isDummyMobile(userMobile)) {
            String assistedMobile = otpRequestDTO.getPrimaryMobile();
            return isBlank(assistedMobile) ? null : assistedMobile.trim();
        }
        return userMobile;
    }

    private boolean isAssistedClaimOtpRequest(OtpRequestDTO otpRequestDTO) {
        return otpRequestDTO != null
                && Boolean.TRUE.equals(otpRequestDTO.getAssistedMode())
                && Messages.CLAIM_REQUEST_OTP_REQUEST.name().equalsIgnoreCase(otpRequestDTO.getMessage());
    }

    private boolean isDummyMobile(String mobile) {
        return mobile != null && mobile.trim().startsWith(DUMMY_MOBILE_PREFIX);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Transactional
    protected ApplicationOtpSession updateOtpSession(String otp, boolean state) {
        try {
            log.info("Processing onboarding otp request  application otp session update {} ", otp);
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setSuccess(state);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("Processing onboarding otp request otp session update {} ", otpSession);
            return otpSession;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateOnboardingVerifiedMobile(OtpRequestDTO otpRequestDTO, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Update record updateOnboardingVerifiedMobile {}", otpRequestDTO);
            OnboardingVerifiedMobile onboardingVerifiedMobile = new OnboardingVerifiedMobile();
            onboardingVerifiedMobile.setNic(otpRequestDTO.getSignupOtp().getNic().trim());
            onboardingVerifiedMobile.setEpfNo(otpRequestDTO.getSignupOtp().getEpfNo().trim());
            onboardingVerifiedMobile.setMobile(otpRequestDTO.getPrimaryMobile().trim());
            onboardingVerifiedMobile.setVerified(false);
            onboardingVerifiedMobile.setApplicationOtpSession(applicationOtpSession);
            onboardingVerifiedMobileRepository.saveAndFlush(onboardingVerifiedMobile);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> otpValidate(OtpValidationDTO otpValidationDTO, Locale locale) {
        try {
            log.info("Otp request  Validate {} ", otpValidationDTO);
            if (otpValidationDTO.getMessage().equalsIgnoreCase(Messages.SIGNUP_OTP_VALIDATION.name())) {
                log.info("Processing SignupOtpValidation {}", otpValidationDTO);
                return applicationOtpSessionRepository.findByOtpAndValidated(otpValidationDTO.getOtp(), false)
                        .map(os -> onboardingVerifiedMobileRepository.findByApplicationOtpSession(os)
                                .map(oss -> applicationPasswordPolicyRepository.findPasswordPolicy()
                                        .map(pw -> applicationUsernamePolicyRepository.findUsernamePolicy()
                                                .map(up -> {
                                                    if (DateTimeUtil.getSeconds(os.getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime()) &&
                                                            os.getOtp().equals(otpValidationDTO.getOtp()) && !os.isValidated()) {
                                                        log.info("Otp request for signup {} ", os);
                                                        updateOtpData(os, oss);
                                                        return ResponseEntity.ok().body(
                                                                responseUtil.success((Object) Map.of("passwordPolicy", gson.fromJson(gson.toJson(pw), PolicyResponseDTO.class), "usernamePolicy", gson.fromJson(gson.toJson(up), PolicyResponseDTO.class)),
                                                                        messageSource.getMessage(ResponseMessageUtil.OTP_VALIDATION_SUCCESS, null, locale))
                                                        );
                                                    }

                                                    log.info("Signup otp request validation fail otp or invalid session {}", os);
                                                    return ResponseEntity.ok().body(
                                                            responseUtil.error(null, 1016,
                                                                    messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale))
                                                    );
                                                })
                                                .orElseGet(() -> {
                                                    log.info("Signup otp mobile verified username policy not found {}", otpValidationDTO);
                                                    return ResponseEntity.ok().body(
                                                            responseUtil.error(null, 1022,
                                                                    messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_NOT_FOUND, null, locale))
                                                    );
                                                })
                                        )
                                        .orElseGet(() -> {
                                            log.info("Signup otp mobile verified password policy not found {}", otpValidationDTO);
                                            return ResponseEntity.ok().body(
                                                    responseUtil.error(null, 1010,
                                                            messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_NOT_FOUND, null, locale))
                                            );
                                        })
                                )
                                .orElseGet(() -> {
                                    log.info("Signup otp mobile verified not found {}", otpValidationDTO);
                                    return ResponseEntity.ok().body(
                                            responseUtil.error(null, 1020,
                                                    messageSource.getMessage(ResponseMessageUtil.ONBOARDING_VERIFICATION_OTP_NOT_FOUND, null, locale))
                                    );
                                })
                        ).orElseGet(() -> {
                            log.info("Signup otp session not found {}", otpValidationDTO);
                            return ResponseEntity.ok().body(
                                    responseUtil.error(null, 1015,
                                            messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale))
                            );
                        });
            }else if(otpValidationDTO.getMessage().equalsIgnoreCase(Messages.RESET_PASSWORD_OTP_VALIDATION.name())
                    || otpValidationDTO.getMessage().equalsIgnoreCase(Messages.CLAIM_REQUEST_OTP_VALIDATION.name())
                    || otpValidationDTO.getMessage().equalsIgnoreCase(Messages.PROFILE_UPDATE_OTP_VALIDATION.name())) {
                log.info("processing otp validation request {}", otpValidationDTO);
                String username = otpValidationDTO.getUsername().trim();
                Optional<ApplicationUser> optionalUser = applicationUserRepository
                        .findByUsernameAndUserPersonalDetails_UserStatus(username,Status.ACTIVE);

                if (optionalUser.isEmpty()) {
                    log.info("OTP validate request find by email {} ", username);
                    optionalUser = applicationUserRepository
                            .findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(username,Status.ACTIVE);
                    otpValidationDTO.setUsername(optionalUser.map(ApplicationUser::getUsername).orElse(""));
                }

                if (optionalUser.isPresent()) {
                    ApplicationUser user = optionalUser.get();
                    if (user.getApplicationOtpSession() != null) {
                        log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                        if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(),60).after(DateTimeUtil.getCurrentDateTime()) &&
                                user.getApplicationOtpSession().getOtp().equals(otpValidationDTO.getOtp()) && !user.getApplicationOtpSession().isValidated()) {
                            log.info("Otp request valid {} ", user.getApplicationOtpSession());
                            updateApplicationUserOtpData(user,user.getApplicationOtpSession());
                            return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.OTP_VALIDATION_SUCCESS, null, locale)));
                        }

                        log.info("Otp request validation fail otp or invalid session {}", user.getApplicationOtpSession());
                        return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                    }

                    log.info("Otp request otp session not found {} ", username);
                    return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));
                }

                log.info("Otp validation request not found for username {} ", username);
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));

            }

            return null;

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUserOtpData(ApplicationUser applicationUser,ApplicationOtpSession applicationOtpSession) {
        log.info("Update otp validation request otp records");
        try {
            applicationUser.setOtpAttemptCount(0);
            applicationOtpSession.setValidated(true);
            applicationUserRepository.saveAndFlush(applicationUser);
            applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateOtpData(ApplicationOtpSession applicationOtpSession, OnboardingVerifiedMobile onboardingVerifiedMobile) {
        log.info("Update sign up otp validation request otp records");
        try {
            applicationOtpSession.setValidated(true);
            onboardingVerifiedMobile.setVerified(true);
            applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            onboardingVerifiedMobileRepository.saveAndFlush(onboardingVerifiedMobile);
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}
