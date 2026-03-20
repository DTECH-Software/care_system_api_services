package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.BiometricEnableRequestDTO;
import com.dtech.auth.dto.request.ChannelMbDeviceDetailsDTO;
import com.dtech.auth.dto.response.BiometricEnableDataResponseDTO;
import com.dtech.auth.dto.response.BiometricEnableResponseDTO;
import com.dtech.auth.enums.Status;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ApplicationUserBiometric;
import com.dtech.auth.model.ApplicationUserDeviceDetails;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.ApplicationUserBiometricRepository;
import com.dtech.auth.repository.ApplicationUserDeviceDetailsRepository;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.service.BiometricService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class BiometricServiceImpl implements BiometricService {

    private static final String REQUEST_MESSAGE_TYPE = "enableBiometricReq";
    private static final String RESPONSE_MESSAGE_TYPE = "enableBiometricRes";
    private static final String MOBILE_CHANNEL = "01";
    private static final String MESSAGE_VERSION = "2.2";

    private static final String SUCCESS_CODE = "dbp-364";
    private static final String INVALID_REQUEST_CODE = "dbp-351";
    private static final String USER_NOT_FOUND_CODE = "dbp-352";
    private static final String USER_MISMATCH_CODE = "dbp-365";
    private static final String INTERNAL_ERROR_CODE = "dbp-500";

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ApplicationUserBiometricRepository applicationUserBiometricRepository;

    @Autowired
    private final ApplicationUserDeviceDetailsRepository applicationUserDeviceDetailsRepository;

    @Override
    @Transactional
    public ResponseEntity<BiometricEnableResponseDTO> enableBiometric(BiometricEnableRequestDTO biometricEnableRequestDTO, Locale locale) {
        try {
            String validationError = validateRequest(biometricEnableRequestDTO);
            if (validationError != null) {
                return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, INVALID_REQUEST_CODE, validationError));
            }

            String authenticatedUsername = authenticatedUsername();
            if (!StringUtils.hasText(authenticatedUsername)) {
                return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, USER_MISMATCH_CODE, "Authenticated user is required"));
            }

            String requestUsername = biometricEnableRequestDTO.getDtechUserId().trim();
            if (!authenticatedUsername.equalsIgnoreCase(requestUsername)) {
                return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, USER_MISMATCH_CODE, "Authenticated user does not match dtechUserId"));
            }

            Optional<ApplicationUser> userOptional = applicationUserRepository
                    .findByUsernameAndUserPersonalDetails_UserStatus(requestUsername, Status.ACTIVE);
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, USER_NOT_FOUND_CODE, "User not found"));
            }

            ApplicationUser applicationUser = userOptional.get();
            ChannelMbDeviceDetailsDTO deviceDetails = decodeDeviceInfo(biometricEnableRequestDTO.getDeviceInfo());
            if (deviceDetails == null || !StringUtils.hasText(deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, INVALID_REQUEST_CODE, "Invalid deviceInfo"));
            }

            ApplicationUserDeviceDetails applicationUserDeviceDetails = upsertUserDeviceDetails(deviceDetails);
            applicationUser.setApplicationUserDeviceDetails(applicationUserDeviceDetails);
            applicationUserRepository.saveAndFlush(applicationUser);

            boolean enableBiometric = Boolean.parseBoolean(biometricEnableRequestDTO.getEnableBiometric().trim());
            String uniqueCode = updateBiometricState(applicationUser, biometricEnableRequestDTO.getAppId(), enableBiometric);

            return ResponseEntity.ok(successResponse(
                    biometricEnableRequestDTO,
                    applicationUser,
                    enableBiometric ? "Successfully Added" : "Successfully Updated",
                    uniqueCode
            ));
        } catch (Exception e) {
            log.error("Biometric enable failed", e);
            return ResponseEntity.ok(errorResponse(biometricEnableRequestDTO, INTERNAL_ERROR_CODE, "Something went wrong. Please try again later"));
        }
    }

    private String validateRequest(BiometricEnableRequestDTO request) {
        if (request == null) {
            return "Request body is required";
        }
        if (!StringUtils.hasText(request.getDtechUserId())) {
            return "dtechUserId is required";
        }
        if (!StringUtils.hasText(request.getEnableBiometric())) {
            return "enableBiometric is required";
        }
        String enableValue = request.getEnableBiometric().trim();
        if (!"true".equalsIgnoreCase(enableValue) && !"false".equalsIgnoreCase(enableValue)) {
            return "enableBiometric must be true or false";
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
            return "messageType must be enableBiometricReq";
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

    private String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? String.valueOf(authentication.getPrincipal()) : null;
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
            dto.setLatitude(StringUtils.hasText(getJsonValue(deviceData, "latitude")) ? getJsonValue(deviceData, "latitude") : "");
            dto.setLongitude(StringUtils.hasText(getJsonValue(deviceData, "longitude")) ? getJsonValue(deviceData, "longitude") : "");
            return dto;
        } catch (Exception e) {
            log.error("Failed to decode biometric deviceInfo", e);
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

    private ApplicationUserDeviceDetails upsertUserDeviceDetails(ChannelMbDeviceDetailsDTO deviceDetails) {
        ApplicationUserDeviceDetails applicationUserDeviceDetails = applicationUserDeviceDetailsRepository
                .findByDeviceId(deviceDetails.getDeviceId())
                .orElseGet(ApplicationUserDeviceDetails::new);

        applicationUserDeviceDetails.setDeviceId(deviceDetails.getDeviceId());
        applicationUserDeviceDetails.setDeviceModel(StringUtils.hasText(deviceDetails.getDeviceModel()) ? deviceDetails.getDeviceModel() : "UNKNOWN");
        applicationUserDeviceDetails.setDeviceOS(StringUtils.hasText(deviceDetails.getDeviceOS()) ? deviceDetails.getDeviceOS() : "UNKNOWN");
        applicationUserDeviceDetails.setDeviceName(StringUtils.hasText(deviceDetails.getDeviceName()) ? deviceDetails.getDeviceName() : "UNKNOWN");
        applicationUserDeviceDetails.setLatitude(StringUtils.hasText(deviceDetails.getLatitude()) ? deviceDetails.getLatitude() : "");
        applicationUserDeviceDetails.setLongitude(StringUtils.hasText(deviceDetails.getLongitude()) ? deviceDetails.getLongitude() : "");
        return applicationUserDeviceDetailsRepository.saveAndFlush(applicationUserDeviceDetails);
    }

    private String updateBiometricState(ApplicationUser applicationUser, String appId, boolean enableBiometric) {
        Optional<ApplicationUserBiometric> biometricOptional = applicationUserBiometricRepository.findByApplicationUser(applicationUser);

        if (!enableBiometric) {
            biometricOptional.ifPresent(applicationUserBiometric -> {
                applicationUserBiometric.setEnabled(false);
                applicationUserBiometric.setAppId(appId);
                applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);
            });
            return null;
        }

        ApplicationUserBiometric applicationUserBiometric = biometricOptional.orElseGet(ApplicationUserBiometric::new);
        applicationUserBiometric.setApplicationUser(applicationUser);
        applicationUserBiometric.setAppId(appId);
        applicationUserBiometric.setEnabled(true);

        String uniqueCode = generateUniqueCode();
        applicationUserBiometric.setUniqueCode(uniqueCode);
        applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);
        return uniqueCode;
    }

    private String generateUniqueCode() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String issuedValue = HexFormat.of().withUpperCase().formatHex(randomBytes);
        return Base64.getEncoder().encodeToString(issuedValue.getBytes(StandardCharsets.UTF_8));
    }

    private BiometricEnableResponseDTO successResponse(BiometricEnableRequestDTO request,
                                                       ApplicationUser applicationUser,
                                                       String responseDescription,
                                                       String uniqueCode) {
        UserPersonalDetails userPersonalDetails = applicationUser.getUserPersonalDetails();
        BiometricEnableDataResponseDTO data = BiometricEnableDataResponseDTO.builder()
                .uniqueCode(uniqueCode)
                .userName(applicationUser.getUsername())
                .mobileNo(applicationUser.getPrimaryMobile())
                .name(buildName(userPersonalDetails))
                .email(applicationUser.getPrimaryEmail())
                .nic(userPersonalDetails != null ? userPersonalDetails.getNic() : null)
                .profileImageKey(applicationUser.getProfileImg() != null ? String.valueOf(applicationUser.getProfileImg().getId()) : null)
                .build();

        return BiometricEnableResponseDTO.builder()
                .messageType(RESPONSE_MESSAGE_TYPE)
                .messageVersion(request.getMessageVersion())
                .deviceChannel(request.getDeviceChannel())
                .dtechTransId(request.getDtechTransId())
                .appTransId(request.getAppTransId())
                .responseCode(SUCCESS_CODE)
                .responseDescription(responseDescription)
                .dtechUserId(request.getDtechUserId())
                .data(data)
                .build();
    }

    private String buildName(UserPersonalDetails userPersonalDetails) {
        if (userPersonalDetails == null) {
            return null;
        }
        String firstName = StringUtils.hasText(userPersonalDetails.getFirstName()) ? userPersonalDetails.getFirstName().trim() : "";
        String lastName = StringUtils.hasText(userPersonalDetails.getLastName()) ? userPersonalDetails.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.hasText(fullName) ? fullName : null;
    }

    private BiometricEnableResponseDTO errorResponse(BiometricEnableRequestDTO request,
                                                     String responseCode,
                                                     String responseDescription) {
        return BiometricEnableResponseDTO.builder()
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
}
