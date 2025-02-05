package com.dtech.login.service;

import com.dtech.login.dto.request.LoginRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface LoginService {
    ResponseEntity<ApiResponse<Object>> loginRequest(LoginRequestDTO loginRequestDTO, Locale locale);
}
