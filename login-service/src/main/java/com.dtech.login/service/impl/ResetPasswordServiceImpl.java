/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 4:55 PM
 * <p>
 */

package com.dtech.login.service.impl;


import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.request.MessageRequestDTO;
import com.dtech.login.dto.request.OtpRequestDTO;
import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.dto.response.MessageResponseDTO;
import com.dtech.login.dto.response.PasswordPolicyResponseDTO;
import com.dtech.login.enums.NotificationsType;
import com.dtech.login.enums.Status;
import com.dtech.login.feign.MessageFeignClient;
import com.dtech.login.model.ApplicationOtpSession;
import com.dtech.login.model.ApplicationPasswordHistory;
import com.dtech.login.model.ApplicationPasswordPolicy;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.repository.*;
import com.dtech.login.service.ResetPasswordService;
import com.dtech.login.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.*;


@Service
@RequiredArgsConstructor
@Log4j2
public class ResetPasswordServiceImpl implements ResetPasswordService {

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> resetRequest(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("Processing reset password request gen otp {} ", channelRequestDTO.getUsername());
            String username = channelRequestDTO.getUsername();
            Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                log.info("Reset password OTP request find by email {} ", username);
                optionalUser = applicationUserRepository.findByPrimaryEmail(username);
                channelRequestDTO.setUsername(optionalUser.isEmpty() ? "" : optionalUser.get().getUsername());
            }

            return optionalUser.map(user -> applicationPasswordPolicyRepository.findPasswordPolicy().map((policy) -> {

                if (user.getOtpAttemptCount() > policy.getOtpExceedCount()) {
                    log.info("Reset password OTP request attempt exceed {} , {}", user.getOtpAttemptCount(), policy.getAttemptExceedCount());
                    long minutes = DateTimeUtil.getMinutes(DateTimeUtil.getYyyyMMddHHMmSsTimeFormatter(DateTimeUtil.getSeconds(user.getOtpAttemptResetTime(), 2700)));
                    return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_EXCEED, new Object[]{minutes}, locale)));
                } else if (user.getOtpAttemptCount() > 0) {
                    log.info("Reset password request otp session {}", user.getApplicationOtpSession());
                    Optional<ApplicationOtpSession> applicationOtpSession = applicationOtpSessionRepository.
                            findById(user.getApplicationOtpSession() != null ? user.getApplicationOtpSession().getId() : 0);

                    if (applicationOtpSession.isPresent()) {
                        log.info("Reset password request otp session {}", applicationOtpSession.get());
                        if (DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(),60).after(DateTimeUtil.getCurrentDateTime())) {
                            log.info("Reset password request otp session valid this moment {}", DateTimeUtil.getSeconds(applicationOtpSession.get().getCreatedDate(),60));
                            return ResponseEntity.ok().body(responseUtil.error(null, 1012, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_REQUEST_TRY_TO_AFTER_60S, null, locale)));
                        }
                        log.info("Rest password send otp session attempt exceed greater than 0 {}", user);
                    } else {
                        log.info("Reset password request otp session not found {}", applicationOtpSession);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1011, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SESSION_NOT_FOUND, null, locale)));
                    }
                }

                log.info("Rest password send otp session send message {}", user);
                Optional<ApplicationPasswordPolicy> passwordPolicy = applicationPasswordPolicyRepository.findPasswordPolicy();
                return sendMessage(user, locale, policy.getOtpExceedCount() - user.getOtpAttemptCount(), passwordPolicy.map(pw -> gson.fromJson(gson.toJson(pw), PasswordPolicyResponseDTO.class)).orElse(null));
            }).orElseGet(() -> {
                log.info("Password reset request policy not found for username {} ", username);
                return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
            })).orElseGet(() -> {
                log.info("Password otp reset password request user not found for username {} ", channelRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1009, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ResponseEntity<ApiResponse<Object>> sendMessage(ApplicationUser applicationUser, Locale locale, int otpExceedCount,PasswordPolicyResponseDTO passwordPolicy) {
        try {
            log.info("Processing reset password request gen otp {} ", applicationUser.getUsername());
            String otp = RandomGeneratorUtil.getRandom6DigitNumber();
            log.info("Generate otp {} ", otp);
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(otp);
            messageRequestDTO.setMobileNo(applicationUser.getPrimaryMobile());
            messageRequestDTO.setType(NotificationsType.PASSWORD_RESET.name());

            log.info("Before token request mapper {} ", messageRequestDTO);
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
            return ResponseEntity.ok().body(responseUtil.success(Map.of("otpRequestAttempt", otpExceedCount,"passwordPolicy",passwordPolicy), messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_OTP_SEND_SUCCESS, null, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApplicationOtpSession updateOtpSession(String otp, int state) {
        try {
            log.info("Processing reset password request gen otp application otp session update {} ", otp);
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setSuccess(state);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("Reset password request otp session update {} ", otpSession);
            return otpSession;
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


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale) {

        try {
            log.info("processing reset password request {}", resetPasswordDTO);
            String username = resetPasswordDTO.getUsername();
            String password = resetPasswordDTO.getConfirmPassword();

            Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                log.info("Reset password find by email {} ", username);
                optionalUser = applicationUserRepository.findByPrimaryEmail(username);
                resetPasswordDTO.setUsername(optionalUser.isEmpty() ? "" : optionalUser.get().getUsername());
            }

            return optionalUser.map(user -> {
                String hashPassword = "";
                try {
                    log.info("processing reset password hash {}", password);
                    hashPassword = PasswordUtil.passwordEncoder(user.getUserKey(), password);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                String message = validAlignCurrentPasswordPolicy(password, user, hashPassword);
                log.info("After reset password validation process {}", message);
                if (message == null || message.trim().isEmpty()) {
                    updateApplicationUser(user, hashPassword);
                    updateApplicationUserPasswordHistory(user, hashPassword);
                    log.info("password reset successfully completed {} {}", password, username);
                    return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.PASSWORD_RESET_SUCCESS, null, locale)));
                }
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, message));
            }).orElseGet(() -> {
                log.info("Processing reset password request user not found for username {} ", resetPasswordDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1008, messageSource.getMessage(ResponseMessageUtil.USERNAME_PASSWORD_INVALID, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> otpValidation(OtpRequestDTO otpRequestDTO, Locale locale) {
        try {
            log.info("processing otp validation request {}", otpRequestDTO);
            String username = otpRequestDTO.getUsername();
            Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                log.info("OTP validate request find by email {} ", username);
                optionalUser = applicationUserRepository.findByPrimaryEmail(username);
                otpRequestDTO.setUsername(optionalUser.isEmpty() ? "" : optionalUser.get().getUsername());
            }

            if (optionalUser.isPresent()) {
                ApplicationUser user = optionalUser.get();
                if (user.getApplicationOtpSession() != null) {
                    log.info("Otp request otp session  {} ", user.getApplicationOtpSession());

                    if (DateTimeUtil.getSeconds(user.getApplicationOtpSession().getCreatedDate(),60).after(DateTimeUtil.getCurrentDateTime()) &&
                            user.getApplicationOtpSession().getOtp().equals(otpRequestDTO.getOtp()) && !user.getApplicationOtpSession().isValidated()) {
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

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }


    @Transactional(readOnly = true)
    protected String validAlignCurrentPasswordPolicy(String password, ApplicationUser applicationUser, String hashPassword) {
        try {
            log.info("Reset password align with current password policy {}", password);
            return applicationPasswordPolicyRepository.findPasswordPolicy()
                    .map(policy -> {
                        int charCount = PasswordUtil.getCharCount(password);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("Reset password invalid max length validation {}", password);
                            return messageSource.getMessage("val.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("Reset password invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = PasswordUtil.countCharsByConditions(password, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("Reset password invalid upper case validation {}", password);
                            return messageSource.getMessage("val.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = PasswordUtil.countCharsByConditions(password, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("Reset password invalid lower case validation {}", password);
                            return messageSource.getMessage("val.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = PasswordUtil.countCharsByConditions(password, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("Reset password invalid min digit validation {}", password);
                            return messageSource.getMessage("val.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = PasswordUtil.countCharsByConditions(password, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("Reset password invalid special char validation {}", password);
                            return messageSource.getMessage("val.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }

                        //check password history in used
                        if (policy.getPasswordHistory() > 0) {
                            Pageable pageable = PageRequest.of(0, policy.getPasswordHistory(), Sort.by(Sort.Order.desc("createdDate")));
                            List<ApplicationPasswordHistory> byApplicationUserAndPasswordEquals = applicationPasswordHistoryRepository.findByApplicationUser(applicationUser, pageable);

                            if (byApplicationUserAndPasswordEquals != null && !byApplicationUserAndPasswordEquals.isEmpty()) {
                                boolean present = byApplicationUserAndPasswordEquals.stream().anyMatch(pw -> pw.getPassword().equals(hashPassword));
                                if (present) {
                                    log.info("Reset password invalid history validation {}", password);
                                    return messageSource.getMessage("val.password.used.history", null, null);

                                }
                            }
                        }
                        log.info("Reset password success validation {}", password);
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("Password request policy not found for username {} ", password);
                        return messageSource.getMessage("val.password.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateApplicationUserOtpData(ApplicationUser applicationUser,ApplicationOtpSession applicationOtpSession) {
        log.info("Update otp validation request otp records");
        applicationUser.setOtpAttemptCount(0);
        applicationOtpSession.setValidated(true);
        applicationUserRepository.saveAndFlush(applicationUser);
        applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
    }

    @Transactional
    protected void updateApplicationUser(ApplicationUser applicationUser, String newHashPassword) {
        try {
            log.info("Updating application user for reset password {}", applicationUser);
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setAttemptCount(0);
            applicationUser.setPassword(newHashPassword);
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
            log.info("Updating application user password history for reset password {}", applicationUser);
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
