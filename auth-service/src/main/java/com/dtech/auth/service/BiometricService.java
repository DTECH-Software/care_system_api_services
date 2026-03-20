package com.dtech.auth.service;

import com.dtech.auth.dto.request.BiometricEnableRequestDTO;
import com.dtech.auth.dto.response.BiometricEnableResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface BiometricService {

    ResponseEntity<BiometricEnableResponseDTO> enableBiometric(BiometricEnableRequestDTO biometricEnableRequestDTO, Locale locale);
}
