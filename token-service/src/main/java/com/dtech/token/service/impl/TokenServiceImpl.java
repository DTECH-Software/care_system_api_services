/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 10:09 AM
 * <p>
 */

package com.dtech.token.service.impl;

import com.dtech.token.dto.request.ChannelRequestDTO;
import com.dtech.token.dto.response.ApiResponse;
import com.dtech.token.dto.response.TokenValidResponseDTO;
import com.dtech.token.enums.Status;
import com.dtech.token.model.ApplicationUser;
import com.dtech.token.model.WebUser;
import com.dtech.token.repository.ApplicationUserRepository;
import com.dtech.token.repository.ApplicationUserSessionRepository;
import com.dtech.token.repository.WebUserRepository;
import com.dtech.token.service.TokenService;
import com.dtech.token.util.JwtUtil;
import com.dtech.token.util.ResponseMessageUtil;
import com.dtech.token.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private static final String WEB_TOKEN_USERNAME_PREFIX = "WEB:";
    private static final List<String> ASSISTED_LOGIN_ROLES = List.of("HRADMIN", "SUPERADMIN");

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ApplicationUserSessionRepository applicationUserSessionRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final WebUserRepository webUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getToken(ChannelRequestDTO channelRequestDTO, Locale locale) {
        try {
            log.info("get token {}", channelRequestDTO.getUsername());
            String requestedUsername = channelRequestDTO.getUsername().trim();

            if (requestedUsername.startsWith(WEB_TOKEN_USERNAME_PREFIX)) {
                String webUsername = requestedUsername.substring(WEB_TOKEN_USERNAME_PREFIX.length());
                Optional<WebUser> webUserOptional = webUserRepository.findByUsernameAndStatus(webUsername, Status.ACTIVE);
                if (webUserOptional.isPresent() && isAllowedWebUser(webUserOptional.get())) {
                    String token = jwtUtil.generateToken(WEB_TOKEN_USERNAME_PREFIX + webUsername);
                    String refreshToken = jwtUtil.generateRefreshToken(WEB_TOKEN_USERNAME_PREFIX + webUsername);
                    long tokenExpiresIn = jwtUtil.getTokenExpiresInSeconds();
                    log.info("Generated assisted token for web user {}", webUsername);

                    return ResponseEntity.ok().body(responseUtil.success(Map.of(
                                    "accessToken", token,
                                    "refreshToken", refreshToken,
                                    "tokenExpiresIn", tokenExpiresIn
                            ),
                            messageSource.getMessage(ResponseMessageUtil.TOKEN_GENERATE_SUCCESS, null, locale)));
                }
            }

            Optional<ApplicationUser> userOptional = applicationUserRepository
                    .findByUsername(requestedUsername);

            if (userOptional.isPresent()) {
                ApplicationUser user = userOptional.get();
                String token = jwtUtil.generateToken(user.getUsername());
                String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
                long tokenExpiresIn = jwtUtil.getTokenExpiresInSeconds();
                log.info("Generated token for user {}", user.getUsername());

                return ResponseEntity.ok().body(
                        responseUtil.success(
                                Map.of(
                                        "accessToken", token,
                                        "refreshToken", refreshToken,
                                        "tokenExpiresIn", tokenExpiresIn
                                ),
                                messageSource.getMessage(ResponseMessageUtil.TOKEN_GENERATE_SUCCESS, null, locale)
                        )
                );
            } else {
                log.info("User not found: {}", channelRequestDTO.getUsername());
                return ResponseEntity.ok().body(
                        responseUtil.error(
                                null,
                                1009,
                                messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)
                        )
                );
            }

        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> validateToken(String token, Locale locale) {

        try {
            log.info("validate token {}", token);
            AtomicBoolean isValid = new AtomicBoolean(false);
            String[] usernameHolder = new String[1];
            applicationUserSessionRepository.findByTokenAndStatus(token,Status.ACTIVE)
                    .ifPresent(applicationUserSession -> {
                        log.info("validate token present {}", token);
                        isValid.set(jwtUtil.validateToken(token));
                        if (isValid.get()) {
                            usernameHolder[0] = applicationUserSession.getApplicationUser() != null
                                    ? applicationUserSession.getApplicationUser().getUsername()
                                    : jwtUtil.extractUsername(token);
                        }
                        if (!isValid.get()) {
                            log.info("validate token fail {} user session {}", token, applicationUserSession);
                            applicationUserSession.setStatus(Status.INACTIVE);
                            applicationUserSessionRepository.saveAndFlush(applicationUserSession);
                            log.info("validate token fail session update success {}", token);
                        }
                    });
            if (!isValid.get()) {
                validateAssistedToken(token, isValid, usernameHolder);
            }
            return ResponseEntity.ok().body(responseUtil.success(
                    new TokenValidResponseDTO(isValid.get(), usernameHolder[0]),
                    messageSource.getMessage(ResponseMessageUtil.TOKEN_VALIDATE_SUCCESS, null, locale)
            ));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    private void validateAssistedToken(String token, AtomicBoolean isValid, String[] usernameHolder) {
        if (!jwtUtil.validateToken(token)) {
            return;
        }
        String subject = jwtUtil.extractUsername(token);
        if (subject == null || !subject.startsWith(WEB_TOKEN_USERNAME_PREFIX)) {
            return;
        }
        String webUsername = subject.substring(WEB_TOKEN_USERNAME_PREFIX.length());
        webUserRepository.findByUsernameAndStatus(webUsername, Status.ACTIVE)
                .filter(this::isAllowedWebUser)
                .ifPresent(webUser -> {
                    isValid.set(true);
                    usernameHolder[0] = webUser.getUsername();
                });
    }

    private boolean isAllowedWebUser(WebUser webUser) {
        return webUser != null
                && webUser.getLoginStatus() == Status.ACTIVE
                && webUser.getStatus() == Status.ACTIVE
                && webUser.getUserRole() != null
                && ASSISTED_LOGIN_ROLES.stream().anyMatch(role -> role.equalsIgnoreCase(webUser.getUserRole().getCode()));
    }

}
