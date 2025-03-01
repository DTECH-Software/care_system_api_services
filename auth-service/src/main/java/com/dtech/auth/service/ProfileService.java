package com.dtech.auth.service;

import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ProfileService {
    ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> addDependents(ClaimDependentRequestDTO claimDependentRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getDependentsDetails(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateProfileImage(ProfileImageUpdateRequestDTO profileImageUpdateRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateProfileDetailsOtpRequest(ProfileEditOtpRequestDTO profileEditOtpRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateProfileOtpValidation(OtpRequestDTO otpRequestDTO, Locale locale);
}
