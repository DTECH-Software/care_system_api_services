/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.claim.service;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.DeathClaimRequestDTO;
import com.dtech.claim.dto.request.DetailsViewRequestDTO;
import com.dtech.claim.dto.request.PaginationRequest;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.search.ClaimHistory;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DeathClaimRequestService {
    ResponseEntity<ApiResponse<Object>> deathClaimReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> deathClaimRequest(DeathClaimRequestDTO deathClaimRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> deathClaimHistoryList(PaginationRequest<ClaimHistory> paginationRequest, Locale locale);
    ResponseEntity<ApiResponse<Object>> deathDetailsFindById(DetailsViewRequestDTO detailsViewRequestDTO, Locale locale);
}
