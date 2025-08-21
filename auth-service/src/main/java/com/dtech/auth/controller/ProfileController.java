/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:06 PM
 * <p>
 */

package com.dtech.auth.controller;

import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.request.validator.*;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.service.ProfileService;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
    public ResponseEntity<ApiResponse<Object>> addDependents(@RequestBody @Valid ClaimDependentRequestValidatorDTO claimDependentRequestValidatorDTO, Locale locale) {
        log.info("Profile add dependent request controller {} ", claimDependentRequestValidatorDTO);
        return profileService.addDependents(gson.fromJson(gson.toJson(claimDependentRequestValidatorDTO), ClaimDependentRequestDTO.class), locale);
    }

    @PostMapping(path = "/dependent-list-view",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle profile dependent view list request ",notes = "Handle profile dependent view list success or failed")
    public ResponseEntity<ApiResponse<Object>> getDependentsDetails(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Profile view list dependent request controller {} ", channelRequestValidatorDTO);
        return profileService.getDependentsDetails(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/update-profile/edit-image",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle profile image update request ",notes = "Handle profile image update success or failed")
    public ResponseEntity<ApiResponse<Object>> updateProfileImage(@RequestBody @Valid ProfileImageUpdateRequestValidatorDTO profileImageUpdateRequestValidatorDTO, Locale locale) {
        log.info("Profile image update request controller {} ", profileImageUpdateRequestValidatorDTO);
        return profileService.updateProfileImage(gson.fromJson(gson.toJson(profileImageUpdateRequestValidatorDTO), ProfileImageUpdateRequestDTO.class), locale);
    }

    @PostMapping(path = "/update-profile",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle update profile details request ",notes = "Update profile details request success or failed")
    public ResponseEntity<ApiResponse<Object>> updateProfileDetails(@RequestBody @Valid ProfileEditRequestValidatorDTO profileEditRequestValidatorDTO, Locale locale) {
        log.info("Update profile details request controller {} ", profileEditRequestValidatorDTO);
        return profileService.updateProfileDetails(gson.fromJson(gson.toJson(profileEditRequestValidatorDTO), ProfileEditRequestDTO.class), locale);
    }

    @PostMapping(path = "/policy-document" , consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle policy view request", notes = "Policy view request success or failed")
    public ResponseEntity<Resource> policyDocument(@RequestBody @Valid PolicyDocumentRequestValidatorDTO policyDocumentRequestValidatorDTO, Locale locale) {
        log.info("Policy view controller: {}", policyDocumentRequestValidatorDTO);
        return profileService.policyDocument(gson.fromJson(gson.toJson(policyDocumentRequestValidatorDTO), PolicyDocumentRequestDTO.class));
    }

    @PostMapping(path = "/update-marital-status",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle update marital status details request ",notes = "Update marital status details request success or failed")
    public ResponseEntity<ApiResponse<Object>> updateMaritalStatus(@RequestBody @Valid MaritalStatusRequestValidatorDTO maritalStatusRequestValidatorDTO, Locale locale) {
        log.info("Update marital status details request controller {} ", maritalStatusRequestValidatorDTO);
        return profileService.updateMaritalStatus(gson.fromJson(gson.toJson(maritalStatusRequestValidatorDTO), MaritalStatusRequestDTO.class), locale);
    }

}
