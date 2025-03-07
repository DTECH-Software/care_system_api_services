package com.dtech.message.service;

import com.dtech.message.dto.request.OtpRequestDTO;
import com.dtech.message.dto.request.OtpValidationDTO;
import com.dtech.message.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface OtpService {
    ResponseEntity<ApiResponse<Object>> otpRequest(OtpRequestDTO otpRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> otpValidate(OtpValidationDTO otpValidationDTO, Locale locale);
}
