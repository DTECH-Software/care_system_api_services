/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:06 PM
 * <p>
 */

package com.dtech.auth.controller;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.request.DependentRequestDTO;
import com.dtech.auth.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.auth.dto.request.validator.DependentRequestValidatorDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.ProfileService;
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
@RequestMapping(path = "api/v1/profile")
@Log4j2
@RequiredArgsConstructor
public class ProfileController {

    @Autowired
    private final ProfileService profileService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle profile request ",notes = "Profile request success or failed")
    public ResponseEntity<ApiResponse<Object>> profile(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Profile request controller {} ", channelRequestValidatorDTO);
        return profileService.profile(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/dependent",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle profile dependent request ",notes = "Handle profile dependent success or failed")
    public ResponseEntity<ApiResponse<Object>> addDependents(@RequestBody @Valid DependentRequestValidatorDTO dependentRequestValidatorDTO, Locale locale) {
        log.info("Profile add dependent request controller {} ", dependentRequestValidatorDTO);
        return profileService.addDependents(gson.fromJson(gson.toJson(dependentRequestValidatorDTO), DependentRequestDTO.class), locale);
    }

}
