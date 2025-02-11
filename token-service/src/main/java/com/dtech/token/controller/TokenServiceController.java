/**
 * User: Himal_J
 * Date: 1/31/2025
 * Time: 3:37 PM
 * <p>
 */

package com.dtech.token.controller;

import com.dtech.token.dto.request.ChannelRequestDTO;
import com.dtech.token.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.token.dto.response.ApiResponse;
import com.dtech.token.service.TokenService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/token")
@Log4j2
@RequiredArgsConstructor
public class TokenServiceController {

    @Autowired
    public final TokenService tokenService;

    @Autowired
    public final Gson gson;

    @PostMapping(path = "/issuer-token")
    @ApiOperation(value = "Get authentication token",notes = "Return the access token(JWT)")
    public ResponseEntity<ApiResponse<Object>> getToken(@RequestBody @Valid ChannelRequestValidatorDTO requestValidatorDTO, Locale locale) {
        log.info("Getting token controller {} ", requestValidatorDTO);
        return tokenService.getToken(gson.fromJson(gson.toJson(requestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @GetMapping(path = "/validate-token")
    @ApiOperation(value = "Check access token is valid",notes = "Token is valid or invalid")
    public ResponseEntity<ApiResponse<Object>> validateToken(@RequestParam(name = "token",required = true) String token, Locale locale) {
        log.info("Getting token validate controller {} ", token);
        return tokenService.validateToken(token, locale);
    }

}
