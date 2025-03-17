/**
 * User: Himal_J
 * Date: 3/16/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.claim.service;

import com.dtech.claim.dto.request.DeathClaimRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DeathClaimRequestService {
    ResponseEntity<ApiResponse<Object>> deathClaimRequest(DeathClaimRequestDTO deathClaimRequestDTO, Locale locale);
}
