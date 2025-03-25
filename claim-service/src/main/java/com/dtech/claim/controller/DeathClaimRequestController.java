/**
 * User: Himal_J
 * Date: 3/25/2025
 * Time: 10:15 AM
 * <p>
 */

package com.dtech.claim.controller;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.DeathClaimRequestDTO;
import com.dtech.claim.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.claim.dto.request.validator.DeathClaimRequestValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.service.DeathClaimRequestService;
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
@RequestMapping(path = "api/v1/death")
@Log4j2
@RequiredArgsConstructor
public class DeathClaimRequestController {

    @Autowired
    private final DeathClaimRequestService deathClaimRequestService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/reference-data",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim request request ",notes = "Death claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> deathClaimReferenceData(@RequestBody @Valid ChannelRequestValidatorDTO channelRequestValidatorDTO, Locale locale) {
        log.info("Death claim request reference data controller {} ", channelRequestValidatorDTO);
        return deathClaimRequestService.deathClaimReferenceData(gson.fromJson(gson.toJson(channelRequestValidatorDTO), ChannelRequestDTO.class), locale);
    }

    @PostMapping(path = "/request",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle death claim request request ",notes = "Death claim request success or failed")
    public ResponseEntity<ApiResponse<Object>> deathClaimRequest(@RequestBody @Valid DeathClaimRequestValidatorDTO deathClaimRequestValidatorDTO, Locale locale) {
        log.info("Death claim request controller {} ", deathClaimRequestValidatorDTO);
        return deathClaimRequestService.deathClaimRequest(gson.fromJson(gson.toJson(deathClaimRequestValidatorDTO), DeathClaimRequestDTO.class), locale);
    }

}
