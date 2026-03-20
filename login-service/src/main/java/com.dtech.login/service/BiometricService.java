package com.dtech.login.service;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface BiometricService {

    ResponseEntity<ApiResponse<Object>> biometricLogin(BiometricLoginRequestDTO biometricLoginRequestDTO, Locale locale);
}
