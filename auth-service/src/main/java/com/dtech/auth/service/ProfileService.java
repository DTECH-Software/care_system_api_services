package com.dtech.auth.service;

import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ProfileService {
    ResponseEntity<ApiResponse<Object>> profile(SignupInquiryDTO signupInquiryDTO, Locale locale);
}
