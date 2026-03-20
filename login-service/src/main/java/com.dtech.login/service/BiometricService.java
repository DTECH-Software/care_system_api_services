package com.dtech.login.service;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.response.BiometricLoginResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface BiometricService {

    ResponseEntity<BiometricLoginResponseDTO> biometricLogin(BiometricLoginRequestDTO biometricLoginRequestDTO, Locale locale);
}
