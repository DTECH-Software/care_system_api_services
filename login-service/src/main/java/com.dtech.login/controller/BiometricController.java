package com.dtech.login.controller;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.service.BiometricService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/biometric")
@Log4j2
@RequiredArgsConstructor
public class BiometricController {

    @Autowired
    private final BiometricService biometricService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle biometric login request", notes = "Biometric login request success or failed")
    public ResponseEntity<ApiResponse<Object>> biometricLogin(@RequestBody BiometricLoginRequestDTO biometricLoginRequestDTO,
                                                              HttpServletRequest servletRequest, Locale locale) {
        servletRequest.setAttribute("care.audit.username", biometricLoginRequestDTO.getUsername());
        log.info("Biometric login request received for username={}", biometricLoginRequestDTO.getUsername());
        return biometricService.biometricLogin(biometricLoginRequestDTO, locale);
    }
}
