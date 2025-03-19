package com.dtech.claim.service;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.PaginationRequest;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.search.ClaimHistory;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface InsuranceClaimRequestService {
    ResponseEntity<ApiResponse<Object>> insuranceClaimRequest(ClaimRequestDTO claimRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> insuranceClaimReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> insuranceClaimHistoryList(PaginationRequest<ClaimHistory> paginationRequest, Locale locale);
 }
