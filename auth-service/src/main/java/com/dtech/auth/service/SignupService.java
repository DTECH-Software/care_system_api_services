package com.dtech.auth.service;

import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.SignupOtpRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SignupService {
    ResponseEntity<ApiResponse<Object>> signupInquiry(SignupInquiryDTO signupInquiryDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signupOtpRequest(SignupOtpRequestDTO signupOtpRequestDTO, Locale locale);
}
