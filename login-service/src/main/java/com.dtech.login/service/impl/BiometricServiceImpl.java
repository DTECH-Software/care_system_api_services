package com.dtech.login.service.impl;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.request.ChannelMbDeviceDetailsDTO;
import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.response.AccessTokenResponseDTO;
import com.dtech.login.dto.response.BiometricLoginDataResponseDTO;
import com.dtech.login.dto.response.BiometricLoginResponseDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.enums.Channel;
import com.dtech.login.enums.Messages;
import com.dtech.login.enums.Status;
import com.dtech.login.feign.TokenFeignClient;
import com.dtech.login.model.ApplicationUser;
import com.dtech.login.model.ApplicationUserBiometric;
import com.dtech.login.model.ApplicationUserDeviceDetails;
import com.dtech.login.model.ApplicationUserSession;
import com.dtech.login.repository.ApplicationUserBiometricRepository;
import com.dtech.login.repository.ApplicationUserDeviceDetailsRepository;
import com.dtech.login.repository.ApplicationUserRepository;
import com.dtech.login.repository.ApplicationUserSessionRepository;
import com.dtech.login.service.BiometricService;
import com.dtech.login.util.DateTimeUtil;
import com.dtech.login.util.ExtractApiResponseUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class BiometricServiceImpl implements BiometricService {

    private static final String REQUEST_MESSAGE_TYPE = "biometricLoginReq";
    private static final String RESPONSE_MESSAGE_TYPE = "biometricLoginRes";
    private static final String MOBILE_CHANNEL = "01";
    private static final String MESSAGE_VERSION = "2.2";

    private static final String SUCCESS_CODE = "dbp-359";
    private static final String SUCCESS_DESCRIPTION = "OK";
    private static final String INVALID_REQUEST_CODE = "dbp-351";
    private static final String USER_NOT_FOUND_CODE = "dbp-352";
    private static final String BIOMETRIC_NOT_ENABLED_CODE = "dbp-353";
    private static final String USER_INACTIVE_CODE = "dbp-354";
    private static final String PASSWORD_EXPIRED_CODE = "dbp-355";
    private static final String INTERNAL_ERROR_CODE = "dbp-500";

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ApplicationUserBiometricRepository applicationUserBiometricRepository;

    @Autowired
    private final ApplicationUserDeviceDetailsRepository applicationUserDeviceDetailsRepository;

    @Autowired
    private final ApplicationUserSessionRepository applicationUserSessionRepository;

    @Autowired
    private final TokenFeignClient tokenFeignClient;

    @Autowired
    private final Gson gson;

    @Override
    @Transactional
    public ResponseEntity<BiometricLoginResponseDTO> biometricLogin(BiometricLoginRequestDTO biometricLoginRequestDTO, Locale locale) {
        try {
            String validationError = validateRequest(biometricLoginRequestDTO);
            if (validationError != null) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, INVALID_REQUEST_CODE, validationError));
            }

            String username = biometricLoginRequestDTO.getDtechUserId().trim();
            Optional<ApplicationUser> userOptional = applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, USER_NOT_FOUND_CODE, "User not found"));
            }

            ApplicationUser applicationUser = userOptional.get();
            Optional<ApplicationUserBiometric> biometricOptional = applicationUserBiometricRepository
                    .findByApplicationUser_UsernameAndUniqueCodeAndEnabledTrue(username, biometricLoginRequestDTO.getUniqueCode().trim());
            if (biometricOptional.isEmpty()) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, BIOMETRIC_NOT_ENABLED_CODE, "Biometric is not enabled or unique code is invalid"));
            }

            if (applicationUser.isReset() || applicationUser.getLoginStatus() == Status.INACTIVE) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, USER_INACTIVE_CODE, "User is inactive or reset is required"));
            }

            if (applicationUser.getPasswordExpiredDate() != null
                    && applicationUser.getPasswordExpiredDate().before(DateTimeUtil.getCurrentDateTime())) {
                updatePasswordExpireLogin(applicationUser);
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, PASSWORD_EXPIRED_CODE, "Password expired"));
            }

            ChannelMbDeviceDetailsDTO deviceDetails = decodeDeviceInfo(biometricLoginRequestDTO.getDeviceInfo());
            if (deviceDetails == null || !StringUtils.hasText(deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, INVALID_REQUEST_CODE, "Invalid deviceInfo"));
            }
            if (!matchesRegisteredDevice(applicationUser, deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, BIOMETRIC_NOT_ENABLED_CODE, "Biometric is not enabled for this device"));
            }

            AccessTokenResponseDTO tokenResponse = issueToken(username, deviceDetails);
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
                return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, INTERNAL_ERROR_CODE, "Failed to issue token"));
            }

            ApplicationUserDeviceDetails applicationUserDeviceDetails = upsertUserDeviceDetails(deviceDetails);
            updateSuccessfulBiometricLogin(applicationUser, applicationUserDeviceDetails);
            updateUserSession(applicationUser, tokenResponse);

            ApplicationUserBiometric applicationUserBiometric = biometricOptional.get();
            applicationUserBiometric.setAppId(biometricLoginRequestDTO.getAppId());
            applicationUserBiometric.setLastUsedDate(DateTimeUtil.getCurrentDateTime());
            applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);

            return ResponseEntity.ok(successPayload(biometricLoginRequestDTO, tokenResponse, applicationUser.getLastLoggedDate()));
        } catch (Exception e) {
            log.error("Biometric login failed", e);
            return ResponseEntity.ok(errorResponse(biometricLoginRequestDTO, INTERNAL_ERROR_CODE, "Something went wrong. Please try again later"));
        }
    }

    private String validateRequest(BiometricLoginRequestDTO request) {
        if (request == null) {
            return "Request body is required";
        }
        if (!StringUtils.hasText(request.getDtechUserId())) {
            return "dtechUserId is required";
        }
        if (!StringUtils.hasText(request.getUniqueCode())) {
            return "uniqueCode is required";
        }
        if (!StringUtils.hasText(request.getDeviceChannel())) {
            return "deviceChannel is required";
        }
        if (!MOBILE_CHANNEL.equals(request.getDeviceChannel().trim())) {
            return "deviceChannel must be 01";
        }
        if (!StringUtils.hasText(request.getMessageVersion())) {
            return "messageVersion is required";
        }
        if (!MESSAGE_VERSION.equals(request.getMessageVersion().trim())) {
            return "messageVersion must be 2.2";
        }
        if (!StringUtils.hasText(request.getMessageType())) {
            return "messageType is required";
        }
        if (!REQUEST_MESSAGE_TYPE.equals(request.getMessageType().trim())) {
            return "messageType must be biometricLoginReq";
        }
        if (!StringUtils.hasText(request.getDeviceInfo())) {
            return "deviceInfo is required";
        }
        if (!StringUtils.hasText(request.getDtechTransId())) {
            return "dtechTransId is required";
        }
        if (!StringUtils.hasText(request.getAppId())) {
            return "appID is required";
        }
        if (!StringUtils.hasText(request.getAppMaxTimeout())) {
            return "appMaxTimeout is required";
        }
        if (!StringUtils.hasText(request.getAppReferenceNumber())) {
            return "appReferenceNumber is required";
        }
        if (!StringUtils.hasText(request.getAppTransId())) {
            return "appTransID is required";
        }
        return null;
    }

    private ChannelMbDeviceDetailsDTO decodeDeviceInfo(String deviceInfo) {
        try {
            String decoded = decodeBase64(deviceInfo);
            JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject deviceData = root.has("DD") && root.get("DD").isJsonObject()
                    ? root.getAsJsonObject("DD")
                    : root;

            ChannelMbDeviceDetailsDTO dto = new ChannelMbDeviceDetailsDTO();
            dto.setDeviceId(getJsonValue(deviceData, "deviceId"));
            dto.setDeviceModel(getJsonValue(deviceData, "deviceModel"));
            String osName = getJsonValue(deviceData, "osName");
            String osVersion = getJsonValue(deviceData, "osVersion");
            dto.setDeviceOS(StringUtils.hasText(osVersion) ? osName + " " + osVersion : osName);
            dto.setDeviceName(getJsonValue(deviceData, "deviceName"));
            dto.setLatitude(getJsonValue(deviceData, "latitude"));
            dto.setLongitude(getJsonValue(deviceData, "longitude"));
            return dto;
        } catch (Exception e) {
            log.error("Failed to decode deviceInfo", e);
            return null;
        }
    }

    private String decodeBase64(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    private String getJsonValue(JsonObject jsonObject, String key) {
        return jsonObject != null && jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? jsonObject.get(key).getAsString()
                : null;
    }

    private boolean matchesRegisteredDevice(ApplicationUser applicationUser, String deviceId) {
        return applicationUser.getApplicationUserDeviceDetails() != null
                && StringUtils.hasText(applicationUser.getApplicationUserDeviceDetails().getDeviceId())
                && applicationUser.getApplicationUserDeviceDetails().getDeviceId().trim().equals(deviceId.trim());
    }

    private AccessTokenResponseDTO issueToken(String username, ChannelMbDeviceDetailsDTO deviceDetails) {
        ChannelRequestDTO channelRequestDTO = new ChannelRequestDTO();
        channelRequestDTO.setUsername(username);
        channelRequestDTO.setChannel(Channel.MB.name());
        channelRequestDTO.setMessage(Messages.SIGN_IN.name());
        channelRequestDTO.setIp("0.0.0.0");
        channelRequestDTO.setDeviceDetails(deviceDetails);

        ResponseEntity<ApiResponse<Object>> tokenEntity = tokenFeignClient.getToken(channelRequestDTO);
        Object responseObject = ExtractApiResponseUtil.extractApiResponse(tokenEntity);
        return gson.fromJson(gson.toJson(responseObject), AccessTokenResponseDTO.class);
    }

    private ApplicationUserDeviceDetails upsertUserDeviceDetails(ChannelMbDeviceDetailsDTO deviceDetails) {
        ApplicationUserDeviceDetails applicationUserDeviceDetails = applicationUserDeviceDetailsRepository
                .findByDeviceId(deviceDetails.getDeviceId())
                .orElseGet(ApplicationUserDeviceDetails::new);

        applicationUserDeviceDetails.setDeviceId(deviceDetails.getDeviceId());
        applicationUserDeviceDetails.setDeviceModel(deviceDetails.getDeviceModel());
        applicationUserDeviceDetails.setDeviceOS(deviceDetails.getDeviceOS());
        applicationUserDeviceDetails.setDeviceName(deviceDetails.getDeviceName());
        applicationUserDeviceDetails.setLatitude(StringUtils.hasText(deviceDetails.getLatitude()) ? deviceDetails.getLatitude() : "");
        applicationUserDeviceDetails.setLongitude(StringUtils.hasText(deviceDetails.getLongitude()) ? deviceDetails.getLongitude() : "");
        return applicationUserDeviceDetailsRepository.saveAndFlush(applicationUserDeviceDetails);
    }

    private void updateSuccessfulBiometricLogin(ApplicationUser applicationUser, ApplicationUserDeviceDetails applicationUserDeviceDetails) {
        applicationUser.setLoginStatus(Status.ACTIVE);
        applicationUser.setLastLoggedChannel(Channel.MB);
        applicationUser.setLastLoggedDate(DateTimeUtil.getCurrentDateTime());
        applicationUser.setMbLastLoggedDate(DateTimeUtil.getCurrentDateTime());
        applicationUser.setApplicationUserDeviceDetails(applicationUserDeviceDetails);
        applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
        applicationUser.setAttemptCount(0);
        applicationUser.setExpectingFirstTimeLogging(false);
        applicationUserRepository.saveAndFlush(applicationUser);
    }

    private void updateUserSession(ApplicationUser applicationUser, AccessTokenResponseDTO accessTokenResponseDTO) {
        applicationUserSessionRepository.deleteAllByApplicationUser(applicationUser);
        ApplicationUserSession applicationUserSession = new ApplicationUserSession();
        applicationUserSession.setStatus(Status.ACTIVE);
        applicationUserSession.setApplicationUser(applicationUser);
        applicationUserSession.setToken(accessTokenResponseDTO.getAccessToken());
        applicationUserSessionRepository.saveAndFlush(applicationUserSession);
    }

    private void updatePasswordExpireLogin(ApplicationUser applicationUser) {
        applicationUser.setLoginStatus(Status.INACTIVE);
        applicationUser.setReset(true);
        applicationUserRepository.saveAndFlush(applicationUser);
    }

    private BiometricLoginResponseDTO successPayload(BiometricLoginRequestDTO request, AccessTokenResponseDTO tokenResponse, Date lastLoggedDate) {
        BiometricLoginDataResponseDTO data = BiometricLoginDataResponseDTO.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenExpiresIn(tokenResponse.getTokenExpiresIn())
                .viewAllOptions(false)
                .lastLoggedDate(formatDate(lastLoggedDate))
                .build();

        return BiometricLoginResponseDTO.builder()
                .messageType(RESPONSE_MESSAGE_TYPE)
                .messageVersion(request.getMessageVersion())
                .deviceChannel(request.getDeviceChannel())
                .dtechTransId(request.getDtechTransId())
                .appTransId(request.getAppTransId())
                .responseCode(SUCCESS_CODE)
                .responseDescription(SUCCESS_DESCRIPTION)
                .dtechUserId(request.getDtechUserId())
                .data(data)
                .build();
    }

    private BiometricLoginResponseDTO errorResponse(BiometricLoginRequestDTO request, String responseCode, String responseDescription) {
        return BiometricLoginResponseDTO.builder()
                .messageType(RESPONSE_MESSAGE_TYPE)
                .messageVersion(request != null ? request.getMessageVersion() : null)
                .deviceChannel(request != null ? request.getDeviceChannel() : null)
                .dtechTransId(request != null ? request.getDtechTransId() : null)
                .appTransId(request != null ? request.getAppTransId() : null)
                .responseCode(responseCode)
                .responseDescription(responseDescription)
                .dtechUserId(request != null ? request.getDtechUserId() : null)
                .build();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
