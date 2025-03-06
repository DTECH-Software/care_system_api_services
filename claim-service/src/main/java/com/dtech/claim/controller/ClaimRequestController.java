/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:44 PM
 * <p>
 */

package com.dtech.claim.controller;

import com.dtech.claim.dto.request.ClaimRequestDTO;
import com.dtech.claim.dto.request.OtpRequestDTO;
import com.dtech.claim.dto.request.validator.ClaimRequestValidatorDTO;
import com.dtech.claim.dto.request.validator.OtpRequestValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.service.ClaimRequestService;
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
@RequestMapping(path = "api/v1/claims")
@Log4j2
@RequiredArgsConstructor
public class ClaimRequestController {

    @Autowired
    private final ClaimRequestService claimRequestService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/request",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle claim request request ",notes = "Claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> claimRequest(@RequestBody @Valid ClaimRequestValidatorDTO claimRequestValidatorDTO, Locale locale) {
        log.info("Claim request controller {} ", claimRequestValidatorDTO);
        return claimRequestService.claimRequest(gson.fromJson(gson.toJson(claimRequestValidatorDTO), ClaimRequestDTO.class), locale);
    }

    @PostMapping(path = "/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle claim request otp request ",notes = "Handle claim request otp request success or failed")
    public ResponseEntity<ApiResponse<Object>> claimRequestOtp(@RequestBody @Valid OtpRequestValidatorDTO otpRequestValidatorDTO, Locale locale) {
        log.info("Claim request otp request controller {} ", otpRequestValidatorDTO);
        return claimRequestService.claimRequestOtp(gson.fromJson(gson.toJson(otpRequestValidatorDTO), OtpRequestDTO.class), locale);
    }

}
