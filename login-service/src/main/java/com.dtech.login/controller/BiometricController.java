package com.dtech.login.controller;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.response.BiometricLoginResponseDTO;
import com.dtech.login.service.BiometricService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BiometricLoginResponseDTO> biometricLogin(@RequestBody BiometricLoginRequestDTO biometricLoginRequestDTO, Locale locale) {
        log.info("Biometric login request controller {}", biometricLoginRequestDTO);
        return biometricService.biometricLogin(biometricLoginRequestDTO, locale);
    }
}
