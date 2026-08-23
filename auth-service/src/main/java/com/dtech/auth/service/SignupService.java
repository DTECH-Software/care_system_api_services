package com.dtech.auth.service;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.UserPersonalDetailsRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SignupService {
    ResponseEntity<ApiResponse<Object>> splash(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signupInquiry(SignupInquiryDTO signupInquiryDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> signup(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO, Locale locale);
}
