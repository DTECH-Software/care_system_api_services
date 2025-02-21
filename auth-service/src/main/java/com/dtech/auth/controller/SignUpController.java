/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:17 AM
 * <p>
 */

package com.dtech.auth.controller;

import com.dtech.auth.dto.request.OtpRequestDTO;
import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.SignupOtpRequestDTO;
import com.dtech.auth.dto.request.validator.OtpRequestValidatorDTO;
import com.dtech.auth.dto.request.validator.SignupInquiryValidatorDTO;
import com.dtech.auth.dto.request.validator.SignupOtpRequestValidatorDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.SignupService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/sign-up")
@Log4j2
@RequiredArgsConstructor
public class SignUpController {

    @Autowired
    private final SignupService signupService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/inquiry",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle signup inquiry request request ",notes = "Inquiry for signup request success or failed")
    public ResponseEntity<ApiResponse<Object>> signupInquiry(@RequestBody @Valid SignupInquiryValidatorDTO signupInquiryValidatorDTO, Locale locale) {
        log.info("Signup inquiry request controller {} ", signupInquiryValidatorDTO);
        return signupService.signupInquiry(gson.fromJson(gson.toJson(signupInquiryValidatorDTO), SignupInquiryDTO.class), locale);
    }

    @PostMapping(path = "/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle signup OTP request request ",notes = "OTP for signup request success or failed")
    public ResponseEntity<ApiResponse<Object>> signupOtpRequest(@RequestBody @Valid SignupOtpRequestValidatorDTO signupOtpRequestValidatorDTO, Locale locale) {
        log.info("Signup OTP request controller {} ", signupOtpRequestValidatorDTO);
        return signupService.signupOtpRequest(gson.fromJson(gson.toJson(signupOtpRequestValidatorDTO), SignupOtpRequestDTO.class), locale);
    }

    @PostMapping(path = "/validate/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle signup OTP validation request request ",notes = "OTP validation for signup request success or failed")
    public ResponseEntity<ApiResponse<Object>> signupOtpValidation(@RequestBody @Valid OtpRequestValidatorDTO otpRequestValidatorDTO, Locale locale) {
        log.info("Signup OTP validation request controller {} ", otpRequestValidatorDTO);
        return signupService.signupOtpValidation(gson.fromJson(gson.toJson(otpRequestValidatorDTO), OtpRequestDTO.class), locale);
    }

}
