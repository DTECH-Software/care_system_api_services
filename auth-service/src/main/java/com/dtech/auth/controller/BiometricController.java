package com.dtech.auth.controller;

import com.dtech.auth.dto.request.BiometricEnableRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.BiometricService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/enableBiometric")
@RequiredArgsConstructor
@Log4j2
public class BiometricController {

    private final BiometricService biometricService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle biometric enable request", notes = "Enable biometric request success or failed")
    public ResponseEntity<ApiResponse<Object>> enableBiometric(@RequestBody BiometricEnableRequestDTO biometricEnableRequestDTO, Locale locale) {
        log.info("Biometric enable request controller {}", biometricEnableRequestDTO);
        return biometricService.enableBiometric(biometricEnableRequestDTO, locale);
    }
}
