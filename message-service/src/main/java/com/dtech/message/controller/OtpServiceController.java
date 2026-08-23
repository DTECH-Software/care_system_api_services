/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 12:55 PM
 * <p>
 */

package com.dtech.message.controller;

import com.dtech.message.dto.request.OtpRequestDTO;
import com.dtech.message.dto.request.OtpValidationDTO;
import com.dtech.message.dto.request.validator.OtpRequestValidatorDTO;
import com.dtech.message.dto.request.validator.OtpValidationValidatorDTO;
import com.dtech.message.dto.response.ApiResponse;
import com.dtech.message.service.OtpService;
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
@RequestMapping(path = "api/v1/otp")
@Log4j2
@RequiredArgsConstructor
public class OtpServiceController {

    @Autowired
    public final OtpService otpService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/send",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle send message request ",notes = "Send message request success or failed")
    public ResponseEntity<ApiResponse<Object>> otpRequest(@RequestBody @Valid OtpRequestValidatorDTO otpRequestValidatorDTO, Locale locale) {
        log.info("OTP send via txt request  controller {} ", otpRequestValidatorDTO );
        return otpService.otpRequest(gson.fromJson(gson.toJson(otpRequestValidatorDTO), OtpRequestDTO.class), locale);
    }

    @PostMapping(path = "/validate",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle  OTP validation request",notes = "OTP validation request success or failed")
    public ResponseEntity<ApiResponse<Object>> otpValidate(@RequestBody @Valid OtpValidationValidatorDTO otpValidationValidatorDTO, Locale locale) {
        log.info("OTP validation request controller {} ", otpValidationValidatorDTO);
        return otpService.otpValidate(gson.fromJson(gson.toJson(otpValidationValidatorDTO), OtpValidationDTO.class), locale);
    }

}
