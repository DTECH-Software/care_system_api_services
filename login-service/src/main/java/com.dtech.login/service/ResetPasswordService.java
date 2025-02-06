package com.dtech.login.service;

import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ResetPasswordService {
    ResponseEntity<ApiResponse<Object>> resetPassword(ResetPasswordDTO resetPasswordDTO, Locale locale);
}
