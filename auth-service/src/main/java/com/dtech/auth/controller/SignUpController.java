/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:17 AM
 * <p>
 */

package com.dtech.auth.controller;

import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.validator.SignupInquiryValidatorDTO;
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

}
