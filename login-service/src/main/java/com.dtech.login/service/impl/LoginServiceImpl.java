/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 9:31 AM
 * <p>
 */

package com.dtech.login.service.impl;

import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.request.LoginRequestDTO;
import com.dtech.login.dto.response.AccessTokenResponseDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.enums.Channel;
import com.dtech.login.enums.Status;
import com.dtech.login.feign.MessageFeignClient;
import com.dtech.login.feign.TokenFeignClient;
import com.dtech.login.mapper.CommonRequestMapper;
import com.dtech.login.model.ApplicationPasswordPolicy;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.model.ApplicationUserSession;
import com.dtech.login.repository.ApplicationPasswordPolicyRepository;
import com.dtech.login.repository.ApplicationUserRepository;
import com.dtech.login.repository.ApplicationUserSessionRepository;
import com.dtech.login.service.LoginService;
import com.dtech.login.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Log4j2
public class LoginServiceImpl implements LoginService {

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Autowired
    private final ApplicationUserSessionRepository applicationUserSessionRepository;

    @Autowired
    private final Gson gson;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;
    @Autowired
    private TokenFeignClient tokenFeignClient;


    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> loginRequest(LoginRequestDTO loginRequestDTO, Locale locale) {

        try {
            log.info("Processing login request username:-{} password:-{} ", loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
            String username = loginRequestDTO.getUsername().trim();
            String password = loginRequestDTO.getPassword().trim();

            Optional<ApplicationUser> optionalUser = applicationUserRepository.findByUsername(username);

            if(optionalUser.isEmpty()) {
                log.info("Login request find by email {} ", username);
                optionalUser = applicationUserRepository.findByPrimaryEmail(username);
                loginRequestDTO.setUsername(optionalUser.isEmpty()?"":optionalUser.get().getUsername());
            }
            return optionalUser.map(user -> {

                String hashPasswordRequest = "";
                try {
                    hashPasswordRequest = PasswordUtil.passwordEncoder(user.getUserKey(), password);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                log.info("password decoder:-{}", hashPasswordRequest);
                if (user.getPassword().equals(hashPasswordRequest)) {

                    Optional<Integer> passwordPolicyAttemptCount = getPasswordPolicyAttemptCount();
                    if (user.getPasswordExpiredDate().before(DateTimeUtil.getCurrentDateTime())) {
                        log.info("User password {} is expired", username);
                        updatePasswordExpireLogin(user);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1003, messageSource.getMessage(ResponseMessageUtil.PASSWORD_EXPIRED_AT_LOGIN_TIME, null, locale)));
                    } else if (passwordPolicyAttemptCount.get() > 0 && user.getAttemptCount() > passwordPolicyAttemptCount.get()) {
                        log.info("User password {} is attempt exceed", user.getAttemptCount());
                        updatePasswordExpireLogin(user);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1004, messageSource.getMessage(ResponseMessageUtil.PASSWORD_ATTEMPT_EXCEED, null, locale)));
                    }
                    ChannelRequestDTO channelRequestDTO = CommonRequestMapper.mapCommonRequest(loginRequestDTO, ChannelRequestDTO.class);
                    log.info("Before token request mapper {} ", channelRequestDTO);
                    ResponseEntity<ApiResponse<Object>> tokenResponse = tokenFeignClient.getToken(channelRequestDTO);
                    log.info("After token response {}", tokenResponse);
                    Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(tokenResponse);
                    log.info("After token mapper response {}", objectApiResponse);
                    updateSuccessLogin(user, loginRequestDTO);
                    log.info("After successful update application user");
                    updateUserSession(user, gson.fromJson(gson.toJson(objectApiResponse), AccessTokenResponseDTO.class));
                    log.info("After successful update application user session");
                    return ResponseEntity.ok().body(responseUtil.success(objectApiResponse, messageSource.getMessage(ResponseMessageUtil.AUTHENTICATION_SUCCESS, null, locale)));

                } else {
                    log.info("Processing login request password mismatch for username {} ", loginRequestDTO.getUsername());
                    updateWrongLogin(user);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseUtil.error(null, 1005, messageSource.getMessage(ResponseMessageUtil.USERNAME_PASSWORD_INVALID, null, locale)));
                }
            }).orElseGet(() -> {
                log.info("Processing login request user not found for username {} ", loginRequestDTO.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseUtil.error(null, 1006, messageSource.getMessage(ResponseMessageUtil.USERNAME_PASSWORD_INVALID, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    protected Optional<Integer> getPasswordPolicyAttemptCount() {
        try {
            log.info("Processing password policy attemptCount ");
            return applicationPasswordPolicyRepository.findPasswordPolicy().map(ApplicationPasswordPolicy::getAttemptExceedCount);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateSuccessLogin(ApplicationUser applicationUser, LoginRequestDTO loginRequestDTO) {
        try {
            log.info("Processing success login request username {}", loginRequestDTO.getUsername());
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setLastLoggedChannel(Channel.valueOf(loginRequestDTO.getChannel()));
            applicationUser.setLastLoggedDate(DateTimeUtil.getCurrentDateTime());
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setAttemptCount(0);
            applicationUserRepository.saveAndFlush(applicationUser);
            log.info("After success login request update data username {}", loginRequestDTO.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateWrongLogin(ApplicationUser applicationUser) {
        try {
            log.info("Processing wrong login request  username {} attempt {} ",
                    applicationUser.getUsername(), applicationUser.getAttemptCount());
            Optional<Integer> attemptCount = getPasswordPolicyAttemptCount();
            if (attemptCount.get() > 0 && applicationUser.getAttemptCount() > attemptCount.get()) {
                log.info("Processing wrong login request username {} attempt {} login status {} ", applicationUser.getUsername()
                        , applicationUser.getAttemptCount(), applicationUser.getLoginStatus());
                applicationUser.setLoginStatus(Status.INACTIVE);
            }
            applicationUser.setAttemptCount(applicationUser.getAttemptCount() + 1);
            applicationUserRepository.saveAndFlush(applicationUser);
            log.info("After wrong login request update data username {}", applicationUser.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updatePasswordExpireLogin(ApplicationUser applicationUser) {
        try {
            log.info("Processing update password login request  username {} attempt {} ",
                    applicationUser.getUsername(), applicationUser.getAttemptCount());
            applicationUser.setLoginStatus(Status.INACTIVE);
            applicationUser.setIsReset(1);
            applicationUserRepository.saveAndFlush(applicationUser);
            log.info("After update password login request update data username {}", applicationUser.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateUserSession(ApplicationUser applicationUser, AccessTokenResponseDTO accessTokenResponseDTO) {
        try {
            log.info("Processing update user session request username {} ", applicationUser.getUsername());
            applicationUserSessionRepository.deleteAllByApplicationUser(applicationUser);
            log.info("After user session all deleted {} ", applicationUser.getUsername());
            ApplicationUserSession applicationUserSession = new ApplicationUserSession();
            applicationUserSession.setStatus(Status.ACTIVE);
            applicationUserSession.setApplicationUser(applicationUser);
            applicationUserSession.setToken(accessTokenResponseDTO.getAccessToken());
            applicationUserSessionRepository.saveAndFlush(applicationUserSession);
            log.info("After update user session update data username {}", applicationUser.getUsername());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
