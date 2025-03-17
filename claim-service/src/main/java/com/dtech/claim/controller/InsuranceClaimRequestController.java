/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:44 PM
 * <p>
 */

package com.dtech.claim.controller;

import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.OtpRequestDTO;
import com.dtech.claim.dto.request.PaginationRequest;
import com.dtech.claim.dto.request.validator.ClaimRequestValidatorDTO;
import com.dtech.claim.dto.request.validator.OtpRequestValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.search.ClaimHistory;
import com.dtech.claim.service.InsuranceClaimRequestService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/claims")
@Log4j2
@RequiredArgsConstructor
public class InsuranceClaimRequestController {

    @Autowired
    private final InsuranceClaimRequestService insuranceClaimRequestService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/request",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle claim request request ",notes = "Claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> claimRequest(@RequestBody @Valid ClaimRequestValidatorDTO claimRequestValidatorDTO, Locale locale) {
        log.info("Claim request controller {} ", claimRequestValidatorDTO);
        return insuranceClaimRequestService.claimRequest(gson.fromJson(gson.toJson(claimRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle claim history request request ",notes = "Claim request history success or failed")
    public ResponseEntity<ApiResponse<Object>> claimHistoryList(@RequestBody @Valid PaginationRequest<ClaimHistory> paginationRequest, Locale locale) {
        log.info("Claim history request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimHistory>>(){}.getType();
        return insuranceClaimRequestService.claimHistoryList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

    @PostMapping(path = "/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle claim request otp request ",notes = "Handle claim request otp request success or failed")
    public ResponseEntity<ApiResponse<Object>> claimRequestOtp(@RequestBody @Valid OtpRequestValidatorDTO otpRequestValidatorDTO, Locale locale) {
        log.info("Claim request otp request controller {} ", otpRequestValidatorDTO);
        return insuranceClaimRequestService.claimRequestOtp(gson.fromJson(gson.toJson(otpRequestValidatorDTO), OtpRequestDTO.class), locale);
    }

}
