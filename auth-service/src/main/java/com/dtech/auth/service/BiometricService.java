package com.dtech.auth.service;

import com.dtech.auth.dto.request.BiometricEnableRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface BiometricService {

    ResponseEntity<ApiResponse<Object>> enableBiometric(BiometricEnableRequestDTO biometricEnableRequestDTO, Locale locale);
}
