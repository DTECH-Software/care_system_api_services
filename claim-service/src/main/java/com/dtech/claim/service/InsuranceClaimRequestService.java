package com.dtech.claim.service;

import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.OtpRequestDTO;
import com.dtech.claim.dto.request.PaginationRequest;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.search.ClaimHistory;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface InsuranceClaimRequestService {
    ResponseEntity<ApiResponse<Object>> claimRequest(ClaimRequestDTO claimRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> claimHistoryList(PaginationRequest<ClaimHistory> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> claimRequestOtp(OtpRequestDTO otpRequestDTO, Locale locale);
}
