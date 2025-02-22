package com.dtech.auth.service;

import com.dtech.auth.dto.request.OtpRequestDTO;
import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.SignupOtpRequestDTO;
import com.dtech.auth.dto.request.UserPersonalDetailsRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SignupService {
    ResponseEntity<ApiResponse<Object>> signupInquiry(SignupInquiryDTO signupInquiryDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signupOtpRequest(SignupOtpRequestDTO signupOtpRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signupOtpValidation(OtpRequestDTO otpRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signup(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO, Locale locale);
}
