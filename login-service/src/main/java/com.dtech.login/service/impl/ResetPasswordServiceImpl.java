/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 4:55 PM
 * <p>
 */

package com.dtech.login.service.impl;


import com.dtech.login.dto.request.MessageRequestDTO;
import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.dto.response.MessageResponseDTO;
import com.dtech.login.dto.response.PolicyResponseDTO;
import com.dtech.login.enums.NotificationsType;
import com.dtech.login.enums.Status;
import com.dtech.login.feign.MessageFeignClient;
import com.dtech.login.model.ApplicationOtpSession;
import com.dtech.login.model.ApplicationPasswordHistory;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.repository.*;
import com.dtech.login.service.ResetPasswordService;
import com.dtech.login.util.*;
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

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


@Service
@RequiredArgsConstructor
@Log4j2
public class ResetPasswordServiceImpl implements ResetPasswordService {
    private static final String RESET_PASSWORD_OTP_PURPOSE = "RESET_PASSWORD";

    @Value("${otp.validity-seconds:300}")
    private int otpValiditySeconds;

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;

    @Autowired
    private final ApplicationPasswordHistoryRepository applicationPasswordHistoryRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final Gson gson;

    @Transactional
    protected ResponseEntity<ApiResponse<Object>> sendMessage(ApplicationUser applicationUser, Locale locale, int otpExceedCount, PolicyResponseDTO passwordPolicy) {
        try {
            log.info("Processing reset password request gen otp {} ", applicationUser.getUsername());
            String otp = RandomGeneratorUtil.getRandom6DigitNumber();
            log.info("Generated OTP for password reset request");
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(otp);
            messageRequestDTO.setMobileNo(applicationUser.getPrimaryMobile());
            messageRequestDTO.setType(NotificationsType.OTP.name());
            log.info("Before calling message service {}", messageFeignClient);
            ResponseEntity<ApiResponse<Object>> messageResponse = messageFeignClient.sendMessage(messageRequestDTO);
            log.info("After response message service {}", messageResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(messageResponse);
            log.info("After message mapper response {}", objectApiResponse);
            MessageResponseDTO messageResponseDTO = gson.fromJson(gson.toJson(objectApiResponse), MessageResponseDTO.class);
            log.info("Otp send status {}", messageResponseDTO);
            ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO != null ? messageResponseDTO.isSuccess() : false);
            updateApplicationUser(applicationUser, applicationOtpSession);
            log.info("Application OTP session updated successfully");
            if(objectApiResponse != null) {
                return ResponseEntity.ok().body(responseUtil.success(Map.of("otpRequestAttempt", otpExceedCount,"passwordPolicy",passwordPolicy), messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SEND_SUCCESS, null, locale)));
            }
            return ResponseEntity.ok().body(responseUtil.error(null, 1019, messageSource.getMessage(ResponseMessageUtil.OTP_SEND_FAILED, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApplicationOtpSession updateOtpSession(String otp, boolean state) {
        try {
            log.info("Updating password reset OTP session");
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setSuccess(state);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("Password reset OTP session updated id={}", otpSession.getId());
            return otpSession;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUser(ApplicationUser applicationUser, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Updating legacy password-reset OTP user id={}", applicationUser.getId());
            applicationUser.setApplicationOtpSession(applicationOtpSession);
            applicationUser.setOtpAttemptCount(applicationUser.getOtpAttemptCount() + 1);
            applicationUser.setOtpAttemptResetTime(DateTimeUtil.getCurrentDateTime());
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale) {

        try {
            log.info("Processing password reset request for username={}", resetPasswordDTO.getUsername());
            String username = resetPasswordDTO.getUsername().trim();
            String password = resetPasswordDTO.getConfirmPassword().trim();

            Optional<ApplicationUser> optionalUser = applicationUserRepository.
                    findByUsernameAndUserPersonalDetails_UserStatus(username,Status.ACTIVE);

            if (optionalUser.isEmpty()) {
                log.info("Reset password find by email {} ", username);
                optionalUser = applicationUserRepository.
                        findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(username,Status.ACTIVE);
                resetPasswordDTO.setUsername(optionalUser.isEmpty() ? "" : optionalUser.get().getUsername());
            }

            return optionalUser.map(user -> {
                Optional<ApplicationOtpSession> currentSession = applicationOtpSessionRepository
                        .findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(user.getId(), RESET_PASSWORD_OTP_PURPOSE);
                if (currentSession.isEmpty() || !isValidatedResetOtp(currentSession.get())) {
                    return ResponseEntity.ok().body(responseUtil.error(null, 1016,
                            messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                }

                String hashPassword = "";
                try {
                    log.info("Processing password hash username={}", username);
                    hashPassword = PasswordUtil.passwordEncoder(user.getUserKey(), password);
                } catch (NoSuchAlgorithmException e) {
                    log.error(e);
                    throw new RuntimeException(e);
                }
                String message = validAlignCurrentPasswordPolicy(password, user, hashPassword);
                log.info("After reset password validation process {}", message);
                if (message == null || message.trim().isEmpty()) {
                    updateApplicationUser(user, hashPassword);
                    updateApplicationUserPasswordHistory(user, hashPassword);
                    consumeResetOtp(user, currentSession.get());
                    log.info("Password reset successfully completed username={}", username);
                    return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PASSWORD_RESET_SUCCESS, null, locale)));
                }
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, message));
            }).orElseGet(() -> {
                log.info("Processing reset password request user not found for username {} ", resetPasswordDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1014, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Transactional(readOnly = true)
    protected String validAlignCurrentPasswordPolicy(String password, ApplicationUser applicationUser, String hashPassword) {
        try {
            log.info("Validating password against current policy");
            return applicationPasswordPolicyRepository.findPasswordPolicy()
                    .map(policy -> {
                        int charCount = StringUtil.getCharCount(password);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("Reset password failed maximum-length validation");
                            return messageSource.getMessage("val.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("Reset password invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = StringUtil.countCharsByConditions(password, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("Reset password failed uppercase validation");
                            return messageSource.getMessage("val.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = StringUtil.countCharsByConditions(password, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("Reset password failed lowercase validation");
                            return messageSource.getMessage("val.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = StringUtil.countCharsByConditions(password, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("Reset password failed number validation");
                            return messageSource.getMessage("val.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = StringUtil.countCharsByConditions(password, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("Reset password failed special-character validation");
                            return messageSource.getMessage("val.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }

                        //check password history in used
                        if (policy.getPasswordHistory() > 0) {
                            Sort sort = Sort.by(Sort.Order.desc("createdDate"));
                            List<ApplicationPasswordHistory> byApplicationUserAndPasswordEquals = applicationPasswordHistoryRepository
                                    .findByApplicationUserAndPassword(applicationUser,hashPassword,sort);

                            LocalDateTime localDateTime = LocalDateTime.now().minusDays(policy.getPasswordHistory());
                            boolean history = byApplicationUserAndPasswordEquals.stream().anyMatch((pw) -> pw.getCreatedDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(localDateTime));

                            if (history) {
                                    log.info("Reset password failed history validation");
                                    return messageSource.getMessage("val.password.used.history", null, null);
                            }
                        }
                        log.info("Reset password passed policy validation");
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("Password policy not found");
                        return messageSource.getMessage("val.password.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private boolean isValidatedResetOtp(ApplicationOtpSession session) {
        return session.isSuccess()
                && session.isValidated()
                && !session.isConsumed()
                && DateTimeUtil.getSeconds(session.getCreatedDate(), otpValiditySeconds)
                .after(DateTimeUtil.getCurrentDateTime());
    }

    private void consumeResetOtp(ApplicationUser user, ApplicationOtpSession session) {
        user.setOtpAttemptCount(0);
        user.setOtpAttemptResetTime(null);
        session.setConsumed(true);
        applicationUserRepository.saveAndFlush(user);
        applicationOtpSessionRepository.saveAndFlush(session);
    }

    @Transactional
    protected void updateApplicationUser(ApplicationUser applicationUser, String newHashPassword) {
        try {
            log.info("Updating application user password userId={}", applicationUser.getId());
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setAttemptCount(0);
            applicationUser.setPassword(newHashPassword);
            applicationUser.setReset(false);
            applicationUser.setLastPasswordChangeDate(DateTimeUtil.getCurrentDateTime());
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUserPasswordHistory(ApplicationUser applicationUser, String newHashPassword) {
        try {
            log.info("Updating password history userId={}", applicationUser.getId());
            ApplicationPasswordHistory applicationPasswordHistory = new ApplicationPasswordHistory();
            applicationPasswordHistory.setApplicationUser(applicationUser);
            applicationPasswordHistory.setPassword(newHashPassword);
            applicationPasswordHistoryRepository.saveAndFlush(applicationPasswordHistory);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
