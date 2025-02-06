/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 10:09 AM
 * <p>
 */

package com.dtech.token.service.impl;

import com.dtech.token.dto.request.ChannelRequestDTO;
import com.dtech.token.dto.response.ApiResponse;
import com.dtech.token.enums.Status;
import com.dtech.token.repository.ApplicationUserSessionRepository;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Log4j2
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final ApplicationUserSessionRepository applicationUserSessionRepository;

    @Autowired
    private final MessageSource messageSource;

    @Override
    public ResponseEntity<ApiResponse<Object>> getToken(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("get token {}", channelRequestDTO.getUsername());
            String token = jwtUtil.generateToken(channelRequestDTO.getUsername());
            log.info("generate token {}", token);
            return ResponseEntity.ok().body(responseUtil.success(Map.of("accessToken", token), messageSource.getMessage(ResponseMessageUtil.TOKEN_GENERATE_SUCCESS, null, locale)));

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
            applicationUserSessionRepository.findByToken(token)
                    .ifPresent(applicationUserSession -> {
                        log.info("validate token present {}", token);
                        isValid.set(jwtUtil.validateToken(token));
                        if (!isValid.get()) {
                            log.info("validate token fail {} user session {}", token, applicationUserSession);
                            applicationUserSession.setStatus(Status.INACTIVE);
                            applicationUserSessionRepository.saveAndFlush(applicationUserSession);
                            log.info("validate token fail session update success {}", token);
                        }
                    });

            return ResponseEntity.ok().body(responseUtil.success(Map.of("isValid", isValid), messageSource.getMessage(ResponseMessageUtil.TOKEN_VALIDATE_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

}
