/**
 * User: Himal_J
 * Date: 2/6/2025
 * Time: 12:15 PM
 * <p>
 */

package com.dtech.login.controller;

import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.request.OtpRequestDTO;
import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.login.dto.request.validator.OtpRequestValidatorDTO;
import com.dtech.login.dto.request.validator.ResetPasswordValidatorDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.service.ResetPasswordService;
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
@RequestMapping(path = "api/v1/password")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResetPasswordController {
    @Autowired
    public final ResetPasswordService resetPasswordService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/reset/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle password request otp generate request ",notes = "Password reset otp request success or failed")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Password reset otp gen request  controller {} ", channelRequestValidatorDTO);
        return resetPasswordService.resetRequest(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/reset",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle password request request ",notes = "Password reset request success or failed")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody @Valid ResetPasswordValidatorDTO resetPasswordValidatorDTO, Locale locale) {
        log.info("Password reset request controller {} ", resetPasswordValidatorDTO);
        return resetPasswordService.resetPassword(gson.fromJson(gson.toJson(resetPasswordValidatorDTO), ResetPasswordDTO.class), locale);
    }

    @PostMapping(path = "/validate/otp",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle OTP validation request request ",notes = "OTP validation request success or failed")
    public ResponseEntity<ApiResponse<Object>> otpValidation(@RequestBody @Valid OtpRequestValidatorDTO otpRequestValidatorDTO, Locale locale) {
        log.info("OTP validation request request controller {} ", otpRequestValidatorDTO);
        return resetPasswordService.otpValidation(gson.fromJson(gson.toJson(otpRequestValidatorDTO), OtpRequestDTO.class), locale);
    }

}
