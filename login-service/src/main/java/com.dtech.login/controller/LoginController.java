/**
 * User: Himal_J
 * Date: 2/4/2025
 * Time: 10:31 AM
 * <p>
 */

package com.dtech.login.controller;

import com.dtech.login.dto.request.LoginRequestDTO;
import com.dtech.login.dto.request.validator.LoginRequestValidatorDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.service.LoginService;
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
@RequestMapping(path = "api/v1/login")
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LoginController {

    @Autowired
    public final LoginService loginService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/login")
    @ApiOperation(value = "Handle login request ",notes = "Login request success or failed")
    public ResponseEntity<ApiResponse<Object>> loginRequest(@RequestBody @Valid LoginRequestValidatorDTO loginRequestValidatorDTO, Locale locale) {
        log.info("Login request login controller {} ", loginRequestValidatorDTO);
        return loginService.loginRequest(gson.fromJson(gson.toJson(loginRequestValidatorDTO), LoginRequestDTO.class), locale);
    }

}
