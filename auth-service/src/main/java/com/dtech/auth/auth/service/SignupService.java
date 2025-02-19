package com.dtech.auth.auth.service;

import com.dtech.auth.auth.dto.request.SignupRequestDTO;
import com.dtech.auth.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SignupService {
    ResponseEntity<ApiResponse<Object>> signUp(SignupRequestDTO signupRequestDTO, Locale locale);
}
