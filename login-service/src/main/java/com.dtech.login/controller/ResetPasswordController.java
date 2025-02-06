/**
 * User: Himal_J
 * Date: 2/6/2025
 * Time: 12:15 PM
 * <p>
 */

package com.dtech.login.controller;

import com.dtech.login.dto.request.ResetPasswordDTO;
import com.dtech.login.dto.request.validator.ResetPasswordValidatorDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.service.ResetPasswordService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/password")
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ResetPasswordController {
    @Autowired
    public final ResetPasswordService resetPasswordService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/reset")
    @ApiOperation(value = "Handle password request request ",notes = "Password reset request success or failed")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody @Valid ResetPasswordValidatorDTO resetPasswordValidatorDTO, Locale locale) {
        log.info("Password reset request login controller {} ", resetPasswordValidatorDTO);
        return resetPasswordService.resetPassword(gson.fromJson(gson.toJson(resetPasswordValidatorDTO), ResetPasswordDTO.class), locale);
    }

}
