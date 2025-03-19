/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:44 PM
 * <p>
 */

package com.dtech.claim.controller;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.PaginationRequest;
import com.dtech.claim.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.claim.dto.request.validator.ClaimRequestValidatorDTO;
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

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle insurance claim request request ",notes = "Insurance claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> insuranceClaimReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Insurance claim request reference data controller {} ", channelRequestValidatorDTO);
        return insuranceClaimRequestService.insuranceClaimReferenceData(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/request",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle insurance claim request request ",notes = "Insurance claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> insuranceClaimRequest(@RequestBody @Valid ClaimRequestValidatorDTO claimRequestValidatorDTO, Locale locale) {
        log.info("Insurance claim request controller {} ", claimRequestValidatorDTO);
        return insuranceClaimRequestService.insuranceClaimRequest(gson.fromJson(gson.toJson(claimRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle insurance claim history request request ",notes = "Insurance claim request history success or failed")
    public ResponseEntity<ApiResponse<Object>> insuranceClaimHistoryList(@RequestBody @Valid PaginationRequest<ClaimHistory> paginationRequest, Locale locale) {
        log.info("Insurance claim history request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<ClaimHistory>>(){}.getType();
        return insuranceClaimRequestService.insuranceClaimHistoryList(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

}
