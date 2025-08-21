package com.dtech.auth.service;

import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ProfileService {
    ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> addDependents(ClaimDependentRequestDTO claimDependentRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getDependentsDetails(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateProfileImage(ProfileImageUpdateRequestDTO profileImageUpdateRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> updateProfileDetails(ProfileEditRequestDTO profileEditRequestDTO, Locale locale);
    ResponseEntity<Resource> policyDocument(PolicyDocumentRequestDTO policyDocumentRequestDTO);
    ResponseEntity<ApiResponse<Object>> updateMaritalStatus(MaritalStatusRequestDTO maritalStatusRequestDTO,Locale locale);
}
