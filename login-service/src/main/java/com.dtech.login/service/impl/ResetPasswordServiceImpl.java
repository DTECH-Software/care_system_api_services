/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 4:55 PM
 * <p>
 */

package com.dtech.login.service.impl;


import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.enums.Status;
import com.dtech.login.model.ApplicationPasswordHistory;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.repository.ApplicationPasswordHistoryRepository;
import com.dtech.login.repository.ApplicationPasswordPolicyRepository;
import com.dtech.login.repository.ApplicationUserRepository;
import com.dtech.login.service.ResetPasswordService;
import com.dtech.login.util.DateTimeUtil;
import com.dtech.login.util.PasswordUtil;
import com.dtech.login.util.ResponseMessageUtil;
import com.dtech.login.util.ResponseUtil;
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
import java.util.List;
import java.util.Locale;

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale) {

        try {
            log.info("processing reset password request {}", resetPasswordDTO);
            String username = resetPasswordDTO.getUsername();
            String password = resetPasswordDTO.getConfirmPassword();

            return applicationUserRepository.findByUsername(username).map(user -> {
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
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, messageSource.getMessage(message, null, locale)));
            }).orElseGet(() -> {
                log.info("Processing reset password request user not found for username {} ", resetPasswordDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1008, messageSource.getMessage(ResponseMessageUtil.USERNAME_PASSWORD_INVALID, null, locale)));
            });
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
