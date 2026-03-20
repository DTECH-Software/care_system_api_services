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
    private static final String INVALID_REQUEST_CODE = "dbp-351";
    private static final String USER_NOT_FOUND_CODE = "dbp-352";
    private static final String USER_MISMATCH_CODE = "dbp-365";
    private static final String INTERNAL_ERROR_CODE = "dbp-500";

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
            String validationError = validateRequest(biometricEnableRequestDTO);
            if (validationError != null) {
                return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(INVALID_REQUEST_CODE), validationError));
            }

            String authenticatedUsername = authenticatedUsername();
            if (!StringUtils.hasText(authenticatedUsername)) {
                return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(USER_MISMATCH_CODE), "Authenticated user is required"));
            }

            String requestUsername = biometricEnableRequestDTO.getUsername().trim();
            if (!authenticatedUsername.equalsIgnoreCase(requestUsername)) {
                return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(USER_MISMATCH_CODE), "Authenticated user does not match username"));
            }

            Optional<ApplicationUser> userOptional = applicationUserRepository
                    .findByUsernameAndUserPersonalDetails_UserStatus(requestUsername, Status.ACTIVE);
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(USER_NOT_FOUND_CODE), "User not found"));
            }

            ApplicationUser applicationUser = userOptional.get();
            ChannelMbDeviceDetailsDTO deviceDetails = biometricEnableRequestDTO.getDeviceDetails();
            if (deviceDetails == null || !StringUtils.hasText(deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(INVALID_REQUEST_CODE), "deviceDetails.deviceId is required"));
            }

            ApplicationUserDeviceDetails applicationUserDeviceDetails = upsertUserDeviceDetails(deviceDetails);
            applicationUser.setApplicationUserDeviceDetails(applicationUserDeviceDetails);
            applicationUserRepository.saveAndFlush(applicationUser);

            boolean enableBiometric = Boolean.TRUE.equals(biometricEnableRequestDTO.getEnable());
            String uniqueCode = updateBiometricState(applicationUser, enableBiometric, deviceDetails.getDeviceId());
            return ResponseEntity.ok(responseUtil.success(successResponse(applicationUser, enableBiometric, uniqueCode), SUCCESS_MESSAGE));
        } catch (Exception e) {
            log.error("Biometric enable failed", e);
            return ResponseEntity.ok(responseUtil.error(null, parseErrorCode(INTERNAL_ERROR_CODE), "Something went wrong. Please try again later"));
        }
    }

    private String validateRequest(BiometricEnableRequestDTO request) {
        if (request == null) {
            return "Request body is required";
        }
        if (!StringUtils.hasText(request.getUsername())) {
            return "username is required";
        }
        if (request.getEnable() == null) {
            return "enable is required";
        }
        if (!StringUtils.hasText(request.getChannel())) {
            return "channel is required";
        }
        if (!Channel.MB.name().equalsIgnoreCase(request.getChannel().trim())) {
            return "channel must be MB";
        }
        if (!StringUtils.hasText(request.getMessage())) {
            return "message is required";
        }
        if (!BIOMETRIC_ENABLE_MESSAGE.equalsIgnoreCase(request.getMessage().trim())) {
            return "message must be BIOMETRIC_ENABLE";
        }
        if (!StringUtils.hasText(request.getIp())) {
            return "ip is required";
        }
        if (request.getDeviceDetails() == null) {
            return "deviceDetails is required";
        }
        return null;
    }

    private String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? String.valueOf(authentication.getPrincipal()) : null;
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

    private int parseErrorCode(String code) {
        return Integer.parseInt(code.replace("dbp-", ""));
    }
}
