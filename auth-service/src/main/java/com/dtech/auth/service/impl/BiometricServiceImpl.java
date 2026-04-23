package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.BiometricEnableRequestDTO;
import com.dtech.auth.dto.request.ChannelMbDeviceDetailsDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.BiometricEnableDataResponseDTO;
import com.dtech.auth.enums.Channel;
import com.dtech.auth.enums.Status;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ApplicationUserBiometric;
import com.dtech.auth.model.ApplicationUserDeviceDetails;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.ApplicationUserBiometricRepository;
import com.dtech.auth.repository.ApplicationUserDeviceDetailsRepository;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.service.BiometricService;
import com.dtech.auth.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class BiometricServiceImpl implements BiometricService {

    private static final String SUCCESS_MESSAGE = "Biometric updated successfully";
    private static final String BIOMETRIC_ENABLE_MESSAGE = "BIOMETRIC_ENABLE";
    private static final int INVALID_REQUEST_CODE = 3501;
    private static final int MISSING_REQUIRED_FIELD_CODE = 3502;
    private static final int INVALID_CHANNEL_CODE = 3503;
    private static final int INVALID_MESSAGE_CODE = 3504;
    private static final int DEVICE_ID_REQUIRED_CODE = 3505;
    private static final int AUTHENTICATED_USER_REQUIRED_CODE = 3601;
    private static final int USER_MISMATCH_CODE = 3602;
    private static final int USER_NOT_FOUND_CODE = 3603;
    private static final int BIOMETRIC_ENABLE_FAILED_CODE = 3604;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final ApplicationUserBiometricRepository applicationUserBiometricRepository;

    @Autowired
    private final ApplicationUserDeviceDetailsRepository applicationUserDeviceDetailsRepository;

    @Autowired
    private final ResponseUtil responseUtil;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> enableBiometric(BiometricEnableRequestDTO biometricEnableRequestDTO, Locale locale) {
        try {
            ValidationFailure validationFailure = validateRequest(biometricEnableRequestDTO);
            if (validationFailure != null) {
                return ResponseEntity.ok(responseUtil.error(null, validationFailure.errorCode(), validationFailure.message()));
            }

            String authenticatedUsername = authenticatedUsername();
            if (!StringUtils.hasText(authenticatedUsername)) {
                return ResponseEntity.ok(responseUtil.error(null, AUTHENTICATED_USER_REQUIRED_CODE, "Authenticated user is required"));
            }

            String requestUsername = biometricEnableRequestDTO.getUsername().trim();
            if (!authenticatedUsername.equalsIgnoreCase(requestUsername)) {
                return ResponseEntity.ok(responseUtil.error(null, USER_MISMATCH_CODE, "Authenticated user does not match username"));
            }

            Optional<ApplicationUser> userOptional = applicationUserRepository
                    .findByUsernameAndUserPersonalDetails_UserStatus(requestUsername, Status.ACTIVE);
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(responseUtil.error(null, USER_NOT_FOUND_CODE, "User not found"));
            }

            ApplicationUser applicationUser = userOptional.get();
            ChannelMbDeviceDetailsDTO deviceDetails = biometricEnableRequestDTO.getDeviceDetails();
            if (deviceDetails == null || !StringUtils.hasText(deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(responseUtil.error(null, DEVICE_ID_REQUIRED_CODE, "deviceDetails.deviceId is required"));
            }

            ApplicationUserDeviceDetails applicationUserDeviceDetails = upsertUserDeviceDetails(deviceDetails);
            applicationUser.setApplicationUserDeviceDetails(applicationUserDeviceDetails);
            applicationUserRepository.saveAndFlush(applicationUser);

            boolean enableBiometric = Boolean.TRUE.equals(biometricEnableRequestDTO.getEnable());
            String uniqueCode = updateBiometricState(applicationUser, enableBiometric, deviceDetails.getDeviceId());
            return ResponseEntity.ok(responseUtil.success(successResponse(applicationUser, enableBiometric, uniqueCode), SUCCESS_MESSAGE));
        } catch (Exception e) {
            log.error("Biometric enable failed", e);
            return ResponseEntity.ok(responseUtil.error(null, BIOMETRIC_ENABLE_FAILED_CODE, "Something went wrong. Please try again later"));
        }
    }

    private ValidationFailure validateRequest(BiometricEnableRequestDTO request) {
        if (request == null) {
            return new ValidationFailure(INVALID_REQUEST_CODE, "Request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "username is required");
        }
        if (request.getEnable() == null) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "enable is required");
        }
        if (!StringUtils.hasText(request.getChannel())) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "channel is required");
        }
        if (!Channel.MB.name().equalsIgnoreCase(request.getChannel().trim())) {
            return new ValidationFailure(INVALID_CHANNEL_CODE, "channel must be MB");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "message is required");
        }
        if (!BIOMETRIC_ENABLE_MESSAGE.equalsIgnoreCase(request.getMessage().trim())) {
            return new ValidationFailure(INVALID_MESSAGE_CODE, "message must be BIOMETRIC_ENABLE");
        }
        if (!StringUtils.hasText(request.getIp())) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "ip is required");
        }
        if (request.getDeviceDetails() == null) {
            return new ValidationFailure(MISSING_REQUIRED_FIELD_CODE, "deviceDetails is required");
        }
        if (!StringUtils.hasText(request.getDeviceDetails().getDeviceId())) {
            return new ValidationFailure(DEVICE_ID_REQUIRED_CODE, "deviceDetails.deviceId is required");
        }
        return null;
    }

    private String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? String.valueOf(authentication.getPrincipal()) : null;
    }

    private ApplicationUserDeviceDetails upsertUserDeviceDetails(ChannelMbDeviceDetailsDTO deviceDetails) {
        var existingDevices = applicationUserDeviceDetailsRepository
                .findAllByDeviceIdOrderByIdAsc(deviceDetails.getDeviceId());

        if (existingDevices.size() > 1) {
            log.warn("Duplicate device rows found for deviceId {}. Reusing the oldest row with id {}",
                    deviceDetails.getDeviceId(), existingDevices.get(0).getId());
        }

        ApplicationUserDeviceDetails applicationUserDeviceDetails = existingDevices.isEmpty()
                ? new ApplicationUserDeviceDetails()
                : existingDevices.get(0);

        applicationUserDeviceDetails.setDeviceId(deviceDetails.getDeviceId());
        applicationUserDeviceDetails.setDeviceModel(StringUtils.hasText(deviceDetails.getDeviceModel()) ? deviceDetails.getDeviceModel() : "UNKNOWN");
        applicationUserDeviceDetails.setDeviceOS(StringUtils.hasText(deviceDetails.getDeviceOS()) ? deviceDetails.getDeviceOS() : "UNKNOWN");
        applicationUserDeviceDetails.setDeviceName(StringUtils.hasText(deviceDetails.getDeviceName()) ? deviceDetails.getDeviceName() : "UNKNOWN");
        applicationUserDeviceDetails.setLatitude(StringUtils.hasText(deviceDetails.getLatitude()) ? deviceDetails.getLatitude() : "");
        applicationUserDeviceDetails.setLongitude(StringUtils.hasText(deviceDetails.getLongitude()) ? deviceDetails.getLongitude() : "");
        return applicationUserDeviceDetailsRepository.saveAndFlush(applicationUserDeviceDetails);
    }

    private String updateBiometricState(ApplicationUser applicationUser, boolean enableBiometric, String deviceId) {
        Optional<ApplicationUserBiometric> biometricOptional = applicationUserBiometricRepository.findByApplicationUser(applicationUser);

        if (!enableBiometric) {
            biometricOptional.ifPresent(applicationUserBiometric -> {
                applicationUserBiometric.setEnabled(false);
                applicationUserBiometric.setUniqueCode(null);
                applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);
            });
            return null;
        }

        ApplicationUserBiometric applicationUserBiometric = biometricOptional.orElseGet(ApplicationUserBiometric::new);
        applicationUserBiometric.setApplicationUser(applicationUser);
        applicationUserBiometric.setAppId(deviceId);
        applicationUserBiometric.setEnabled(true);
        String uniqueCode = generateUniqueCode(applicationUser.getUsername(), deviceId);
        applicationUserBiometric.setUniqueCode(uniqueCode);
        applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);
        return uniqueCode;
    }

    private String generateUniqueCode(String username, String deviceId) {
        return deviceId + username;
    }

    private BiometricEnableDataResponseDTO successResponse(ApplicationUser applicationUser,
                                                           boolean enableBiometric,
                                                           String uniqueCode) {
        UserPersonalDetails userPersonalDetails = applicationUser.getUserPersonalDetails();
        return BiometricEnableDataResponseDTO.builder()
                .username(applicationUser.getUsername())
                .enable(enableBiometric)
                .uniqueCode(uniqueCode)
                .mobileNo(applicationUser.getPrimaryMobile())
                .name(buildName(userPersonalDetails))
                .email(applicationUser.getPrimaryEmail())
                .nic(userPersonalDetails != null ? userPersonalDetails.getNic() : null)
                .profileImageKey(applicationUser.getProfileImg() != null ? String.valueOf(applicationUser.getProfileImg().getId()) : null)
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

    private record ValidationFailure(int errorCode, String message) {
    }
}
