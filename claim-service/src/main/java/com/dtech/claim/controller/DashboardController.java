/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 9:04 AM
 * <p>
 */

package com.dtech.claim.controller;

import com.dtech.claim.dto.request.DashboardSummaryDTO;
import com.dtech.claim.dto.request.validator.DashboardSummaryValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.service.DashboardService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/dashboard")
@Log4j2
@RequiredArgsConstructor
public class DashboardController {

    @Autowired
    private final DashboardService dashboardService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/summary",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle dashboard summary request request ",notes = "Dashboard summary request success or failed")
    public ResponseEntity<ApiResponse<Object>> dashboardSummary(@RequestBody @Valid DashboardSummaryValidatorDTO dashboardSummaryValidatorDTO, Locale locale) {
        log.info("Dashboard summary request reference data controller {} ", dashboardSummaryValidatorDTO);
        return dashboardService.dashboardSummary(gson.fromJson(gson.toJson(dashboardSummaryValidatorDTO), DashboardSummaryDTO.class), locale);
    }

}
