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
import com.dtech.message.enums.OtpPurpose;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final String DUMMY_MOBILE_PREFIX = "0000";

    @Value("${otp.validity-seconds:300}")
    private int otpValiditySeconds;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int otpResendCooldownSeconds;

    @Value("${otp.lockout-seconds:2700}")
    private int otpLockoutSeconds;

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
    @Transactional
    public ResponseEntity<ApiResponse<Object>> otpRequest(OtpRequestDTO otpRequestDTO, Locale locale) {
        try {
            log.info("OTP request received message={} username={}", otpRequestDTO.getMessage(), otpRequestDTO.getUsername());
            if (otpRequestDTO.getMessage().equalsIgnoreCase(Messages.SIGNUP_OTP_REQUEST.name())) {
                log.info("Processing signup OTP request");
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
                                    String contextKey = signupContextKey(otpRequestDTO);
                                    Optional<ApplicationOtpSession> latestSession = applicationOtpSessionRepository
                                            .findTopByContextKeyAndPurposeOrderByCreatedDateDesc(contextKey, OtpPurpose.SIGNUP.name());
                                    if (latestSession.isPresent() && isWithinSeconds(latestSession.get(), otpResendCooldownSeconds)) {
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1012,
                                                messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S, null, locale)));
                                    }

                                    String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                                    MessageResponseDTO messageResponseDTO = sendMessageService.sendToCustomer(new MessageRequestDTO(otpRequestDTO.getPrimaryMobile(), MessageType.OTP.name(), otp));
                                    if (!messageResponseDTO.isSuccess()) {
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1038, messageResponseDTO.getMessage()));
                                    }

                                    applicationOtpSessionRepository.consumeActiveContextSessions(contextKey, OtpPurpose.SIGNUP.name());
                                    ApplicationOtpSession applicationOtpSession = updateOtpSession(
                                            otp, OtpPurpose.SIGNUP.name(), null, contextKey);
                                    updateOnboardingVerifiedMobile(otpRequestDTO, applicationOtpSession);
                                    log.info("Signup OTP accepted by SMS provider sessionId={}", applicationOtpSession.getId());
                                    return ResponseEntity.ok().body(responseUtil.success((Object) otpResponse(applicationOtpSession), messageResponseDTO.getMessage()));

                                })
                                .orElseGet(() -> {
                                    log.info("Signup OTP password policy not found");
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
                                }))
                        .orElseGet(() -> {
                            log.info("Signup OTP employee details not found");
                            return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{otpRequestDTO.getPrimaryMobile()}, locale)));
                        });
            } else if (otpRequestDTO.getMessage().equalsIgnoreCase(Messages.RESET_PASSWORD_OTP_REQUEST.name())
                    || otpRequestDTO.getMessage().equalsIgnoreCase(Messages.CLAIM_REQUEST_OTP_REQUEST.name())
                    || otpRequestDTO.getMessage().equalsIgnoreCase(Messages.PROFILE_UPDATE_OTP_REQUEST.name())){

                log.info("Processing reset password request gen otp {} ", otpRequestDTO.getUsername());
                String username = otpRequestDTO.getUsername().trim();
                Optional<ApplicationUser> optionalUser = applicationUserRepository.findForOtpUpdateByUsername(username, Status.ACTIVE);

                if (optionalUser.isEmpty()) {
                    log.info("Reset password OTP request find by email {} ", username);
                    optionalUser = applicationUserRepository.findForOtpUpdateByEmail(username, Status.ACTIVE);
                    otpRequestDTO.setUsername(optionalUser.map(ApplicationUser::getUsername).orElse(""));
                }

                return optionalUser.map(user -> applicationPasswordPolicyRepository.findPasswordPolicy().map((policy) -> {

                    String purpose = resolvePurpose(otpRequestDTO.getMessage());
                    resetAttemptCounterAfterLockout(user, policy.getOtpExceedCount());

                    if (policy.getOtpExceedCount() > 0 && user.getOtpAttemptCount() >= policy.getOtpExceedCount()) {
                        log.info("Reset password OTP request attempt exceed {} , {}", user.getOtpAttemptCount(), policy.getOtpExceedCount());
                        long minutes = remainingLockoutMinutes(user);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_EXCEED, new Object[]{minutes}, locale)));
                    }

                    Optional<ApplicationOtpSession> latestOtpSession = applicationOtpSessionRepository
                            .findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(user.getId(), purpose);
                    if (latestOtpSession.isPresent()
                            && isWithinSeconds(latestOtpSession.get(), otpResendCooldownSeconds)) {
                        return ResponseEntity.ok().body(responseUtil.error(null, 1012,
                                messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S, null, locale)));
                    }

                    String otp = RandomGeneratorUtil.getRandom6DigitNumber();
                    String deliveryMobile = resolveOtpDeliveryMobile(otpRequestDTO, user);
                    if (deliveryMobile == null) {
                        return ResponseEntity.ok().body(responseUtil.error(null, 1038,
                                "This employee does not have a registered mobile number. Please enter an assisted mobile number."));
                    }
                    MessageResponseDTO messageResponseDTO = sendMessageService.sendToCustomer(new MessageRequestDTO(deliveryMobile, MessageType.OTP.name(), otp));
                    if (!messageResponseDTO.isSuccess()) {
                        return ResponseEntity.ok().body(responseUtil.error(null, 1038, messageResponseDTO.getMessage()));
                    }

                    applicationOtpSessionRepository.consumeActiveUserSessions(user.getId(), purpose);
                    ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, purpose, user.getId(), null);
                    updateApplicationUser(user, applicationOtpSession);
                    Map<String, Object> response = otpResponse(applicationOtpSession);
                    response.put("otpRequestAttempt", Math.max(0, policy.getOtpExceedCount() - user.getOtpAttemptCount()));
                    log.info("OTP accepted by SMS provider purpose={} sessionId={}", purpose, applicationOtpSession.getId());
                    return ResponseEntity.ok().body(responseUtil.success((Object) response, messageResponseDTO.getMessage()));
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
    protected ApplicationOtpSession updateOtpSession(String otp, String purpose, Long applicationUserId, String contextKey) {
        try {
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setPurpose(purpose);
            applicationOtpSession.setApplicationUserId(applicationUserId);
            applicationOtpSession.setContextKey(contextKey);
            applicationOtpSession.setSuccess(true);
            applicationOtpSession.setValidated(false);
            applicationOtpSession.setConsumed(false);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("OTP session created id={} purpose={}", otpSession.getId(), purpose);
            return otpSession;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateOnboardingVerifiedMobile(OtpRequestDTO otpRequestDTO, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Creating onboarding mobile verification for OTP session id={}", applicationOtpSession.getId());
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
            log.info("OTP validation received message={} username={} sessionId={}",
                    otpValidationDTO.getMessage(), otpValidationDTO.getUsername(), otpValidationDTO.getOtpSessionId());
            if (otpValidationDTO.getMessage().equalsIgnoreCase(Messages.SIGNUP_OTP_VALIDATION.name())) {
                log.info("Processing signup OTP validation");
                return findSignupOtpSession(otpValidationDTO)
                        .map(os -> onboardingVerifiedMobileRepository.findByApplicationOtpSession(os)
                                .map(oss -> applicationPasswordPolicyRepository.findPasswordPolicy()
                                        .map(pw -> applicationUsernamePolicyRepository.findUsernamePolicy()
                                                .map(up -> {
                                                    if (isValidForInitialValidation(os, otpValidationDTO.getOtp())) {
                                                        updateOtpData(os, oss);
                                                        return ResponseEntity.ok().body(
                                                                responseUtil.success((Object) Map.of("passwordPolicy", gson.fromJson(gson.toJson(pw), PolicyResponseDTO.class), "usernamePolicy", gson.fromJson(gson.toJson(up), PolicyResponseDTO.class)),
                                                                        messageSource.getMessage(ResponseMessageUtil.OTP_VALIDATION_SUCCESS, null, locale))
                                                        );
                                                    }

                                                    log.info("Signup OTP invalid or expired sessionId={}", os.getId());
                                                    return ResponseEntity.ok().body(
                                                            responseUtil.error(null, 1016,
                                                                    messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale))
                                                    );
                                                })
                                                .orElseGet(() -> {
                                                    log.info("Signup OTP username policy not found");
                                                    return ResponseEntity.ok().body(
                                                            responseUtil.error(null, 1022,
                                                                    messageSource.getMessage(ResponseMessageUtil.USERNAME_POLICY_NOT_FOUND, null, locale))
                                                    );
                                                })
                                        )
                                        .orElseGet(() -> {
                                            log.info("Signup OTP password policy not found");
                                            return ResponseEntity.ok().body(
                                                    responseUtil.error(null, 1010,
                                                            messageSource.getMessage(ResponseMessageUtil.PASSWORD_POLICY_NOT_FOUND, null, locale))
                                            );
                                        })
                                )
                                .orElseGet(() -> {
                                    log.info("Signup OTP onboarding verification record not found");
                                    return ResponseEntity.ok().body(
                                            responseUtil.error(null, 1020,
                                                    messageSource.getMessage(ResponseMessageUtil.ONBOARDING_VERIFICATION_OTP_NOT_FOUND, null, locale))
                                    );
                                })
                        ).orElseGet(() -> {
                            log.info("Signup OTP session not found sessionId={}", otpValidationDTO.getOtpSessionId());
                            return ResponseEntity.ok().body(
                                    responseUtil.error(null, 1015,
                                            messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale))
                            );
                        });
            }else if(otpValidationDTO.getMessage().equalsIgnoreCase(Messages.RESET_PASSWORD_OTP_VALIDATION.name())
                    || otpValidationDTO.getMessage().equalsIgnoreCase(Messages.CLAIM_REQUEST_OTP_VALIDATION.name())
                    || otpValidationDTO.getMessage().equalsIgnoreCase(Messages.PROFILE_UPDATE_OTP_VALIDATION.name())) {
                log.info("Processing OTP validation message={}", otpValidationDTO.getMessage());
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
                    String purpose = resolvePurpose(otpValidationDTO.getMessage());
                    Optional<ApplicationOtpSession> currentSession = applicationOtpSessionRepository
                            .findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(user.getId(), purpose);
                    if (currentSession.isPresent()) {
                        ApplicationOtpSession session = currentSession.get();
                        if (matchesRequestedSession(session, otpValidationDTO.getOtpSessionId())
                                && isValidForInitialValidation(session, otpValidationDTO.getOtp())) {
                            updateApplicationUserOtpData(user, session);
                            return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.OTP_VALIDATION_SUCCESS, null, locale)));
                        }

                        log.info("OTP invalid or expired purpose={} sessionId={}", purpose, session.getId());
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
            applicationUser.setOtpAttemptResetTime(null);
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
            applicationOtpSession.setConsumed(true);
            onboardingVerifiedMobile.setVerified(true);
            applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            onboardingVerifiedMobileRepository.saveAndFlush(onboardingVerifiedMobile);
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private Optional<ApplicationOtpSession> findSignupOtpSession(OtpValidationDTO request) {
        if (request.getOtpSessionId() != null) {
            return applicationOtpSessionRepository.findById(request.getOtpSessionId())
                    .filter(session -> OtpPurpose.SIGNUP.name().equals(session.getPurpose()));
        }
        return applicationOtpSessionRepository
                .findTopByOtpAndPurposeAndValidatedAndConsumedFalseOrderByCreatedDateDesc(
                        request.getOtp(), OtpPurpose.SIGNUP.name(), false);
    }

    private boolean isValidForInitialValidation(ApplicationOtpSession session, String otp) {
        return session.isSuccess()
                && !session.isValidated()
                && !session.isConsumed()
                && session.getOtp().equals(otp)
                && isWithinSeconds(session, otpValiditySeconds);
    }

    private boolean matchesRequestedSession(ApplicationOtpSession session, Long requestedSessionId) {
        return requestedSessionId == null || requestedSessionId.equals(session.getId());
    }

    private boolean isWithinSeconds(ApplicationOtpSession session, int seconds) {
        return session.getCreatedDate() != null
                && DateTimeUtil.getSeconds(session.getCreatedDate(), seconds).after(DateTimeUtil.getCurrentDateTime());
    }

    private void resetAttemptCounterAfterLockout(ApplicationUser user, int maximumAttempts) {
        if (maximumAttempts <= 0 || user.getOtpAttemptCount() <= 0) {
            return;
        }
        if (user.getOtpAttemptResetTime() == null
                || !DateTimeUtil.getSeconds(user.getOtpAttemptResetTime(), otpLockoutSeconds)
                .after(DateTimeUtil.getCurrentDateTime())) {
            user.setOtpAttemptCount(0);
            user.setOtpAttemptResetTime(null);
            applicationUserRepository.saveAndFlush(user);
        }
    }

    private long remainingLockoutMinutes(ApplicationUser user) {
        if (user.getOtpAttemptResetTime() == null) {
            return 1;
        }
        long lockUntil = DateTimeUtil.getSeconds(user.getOtpAttemptResetTime(), otpLockoutSeconds).getTime();
        long remainingMillis = Math.max(0, lockUntil - DateTimeUtil.getCurrentDateTime().getTime());
        return Math.max(1, (remainingMillis + 59_999) / 60_000);
    }

    private String resolvePurpose(String message) {
        if (Messages.SIGNUP_OTP_REQUEST.name().equalsIgnoreCase(message)
                || Messages.SIGNUP_OTP_VALIDATION.name().equalsIgnoreCase(message)) {
            return OtpPurpose.SIGNUP.name();
        }
        if (Messages.RESET_PASSWORD_OTP_REQUEST.name().equalsIgnoreCase(message)
                || Messages.RESET_PASSWORD_OTP_VALIDATION.name().equalsIgnoreCase(message)) {
            return OtpPurpose.RESET_PASSWORD.name();
        }
        if (Messages.CLAIM_REQUEST_OTP_REQUEST.name().equalsIgnoreCase(message)
                || Messages.CLAIM_REQUEST_OTP_VALIDATION.name().equalsIgnoreCase(message)) {
            return OtpPurpose.CLAIM_REQUEST.name();
        }
        if (Messages.PROFILE_UPDATE_OTP_REQUEST.name().equalsIgnoreCase(message)
                || Messages.PROFILE_UPDATE_OTP_VALIDATION.name().equalsIgnoreCase(message)) {
            return OtpPurpose.PROFILE_UPDATE.name();
        }
        throw new IllegalArgumentException("Unsupported OTP message: " + message);
    }

    private String signupContextKey(OtpRequestDTO request) {
        return String.join("|",
                request.getSignupOtp().getEpfNo().trim().toLowerCase(Locale.ROOT),
                request.getSignupOtp().getNic().trim().toLowerCase(Locale.ROOT),
                request.getPrimaryMobile().trim());
    }

    private Map<String, Object> otpResponse(ApplicationOtpSession session) {
        Map<String, Object> response = new HashMap<>();
        response.put("otpSessionId", session.getId());
        response.put("expiresInSeconds", otpValiditySeconds);
        response.put("resendAfterSeconds", otpResendCooldownSeconds);
        return response;
    }
}
