package com.dtech.claim.service;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface DashboardService {
    ResponseEntity<ApiResponse<Object>> dashboardSummary(DashboardSummaryDTO dashboardSummaryDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> dashboardReferenceData(ChannelRequestDTO channelRequestDTO, Locale locale);
}
