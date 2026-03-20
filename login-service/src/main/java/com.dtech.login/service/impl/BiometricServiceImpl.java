package com.dtech.login.service.impl;

import com.dtech.login.dto.request.BiometricLoginRequestDTO;
import com.dtech.login.dto.request.ChannelMbDeviceDetailsDTO;
import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.response.AccessTokenResponseDTO;
import com.dtech.login.dto.response.ApiResponse;
import com.dtech.login.dto.response.ApplicationUserDetailsResponseDTO;
import com.dtech.login.enums.Channel;
import com.dtech.login.enums.Messages;
import com.dtech.login.enums.Status;
import com.dtech.login.feign.AuthFeignClient;
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
import com.dtech.login.util.ResponseMessageUtil;
import com.dtech.login.util.ResponseUtil;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class BiometricServiceImpl implements BiometricService {

    private static final String BIOMETRIC_LOGIN_MESSAGE = "BIOMETRIC_LOGIN";

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
    private final AuthFeignClient authFeignClient;

    @Autowired
    private final Gson gson;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> biometricLogin(BiometricLoginRequestDTO biometricLoginRequestDTO, Locale locale) {
        try {
            String validationError = validateRequest(biometricLoginRequestDTO);
            if (validationError != null) {
                return ResponseEntity.ok(responseUtil.error(null, 351, validationError));
            }

            String username = biometricLoginRequestDTO.getUsername().trim();
            Optional<ApplicationUser> userOptional = applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(username, Status.ACTIVE);
            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(responseUtil.error(null, 352, "User not found"));
            }

            ApplicationUser applicationUser = userOptional.get();
            Optional<ApplicationUserBiometric> biometricOptional = applicationUserBiometricRepository
                    .findByApplicationUser_UsernameAndUniqueCodeAndEnabledTrue(username, biometricLoginRequestDTO.getUniqueCode().trim());
            if (biometricOptional.isEmpty()) {
                return ResponseEntity.ok(responseUtil.error(null, 353, "Biometric is not enabled or unique code is invalid"));
            }

            if (applicationUser.isReset() || applicationUser.getLoginStatus() == Status.INACTIVE) {
                return ResponseEntity.ok(responseUtil.error(null, 354, "User is inactive or reset is required"));
            }

            if (applicationUser.getPasswordExpiredDate() != null
                    && applicationUser.getPasswordExpiredDate().before(DateTimeUtil.getCurrentDateTime())) {
                updatePasswordExpireLogin(applicationUser);
                return ResponseEntity.ok(responseUtil.error(null, 355, "Password expired"));
            }

            ChannelMbDeviceDetailsDTO deviceDetails = biometricLoginRequestDTO.getDeviceDetails();
            if (deviceDetails == null || !StringUtils.hasText(deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(responseUtil.error(null, 351, "deviceDetails.deviceId is required"));
            }
            if (!matchesRegisteredDevice(applicationUser, deviceDetails.getDeviceId())) {
                return ResponseEntity.ok(responseUtil.error(null, 353, "Biometric is not enabled for this device"));
            }

            AccessTokenResponseDTO tokenResponse = issueToken(biometricLoginRequestDTO, deviceDetails);
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
                return ResponseEntity.ok(responseUtil.error(null, 500, "Failed to issue token"));
            }

            ApplicationUserDeviceDetails applicationUserDeviceDetails = upsertUserDeviceDetails(deviceDetails);
            updateSuccessfulBiometricLogin(applicationUser, applicationUserDeviceDetails);
            updateUserSession(applicationUser, tokenResponse);

            ApplicationUserBiometric applicationUserBiometric = biometricOptional.get();
            applicationUserBiometric.setAppId(deviceDetails.getDeviceId());
            applicationUserBiometric.setLastUsedDate(DateTimeUtil.getCurrentDateTime());
            applicationUserBiometricRepository.saveAndFlush(applicationUserBiometric);

            ApplicationUserDetailsResponseDTO userProfileDetails = getUserProfileDetails(username, biometricLoginRequestDTO);
            userProfileDetails.setAccessToken(tokenResponse.getAccessToken());
            updateApplicationUserDetails(applicationUser);
            return ResponseEntity.ok(responseUtil.success((Object) userProfileDetails,
                    messageSource.getMessage(ResponseMessageUtil.AUTHENTICATION_SUCCESS, null, locale)));
        } catch (Exception e) {
            log.error("Biometric login failed", e);
            return ResponseEntity.ok(responseUtil.error(null, 500, "Something went wrong. Please try again later"));
        }
    }

    private String validateRequest(BiometricLoginRequestDTO request) {
        if (request == null) {
            return "Request body is required";
        }
        if (!StringUtils.hasText(request.getUsername())) {
            return "username is required";
        }
        if (!StringUtils.hasText(request.getUniqueCode())) {
            return "uniqueCode is required";
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
        if (!BIOMETRIC_LOGIN_MESSAGE.equalsIgnoreCase(request.getMessage().trim())) {
            return "message must be BIOMETRIC_LOGIN";
        }
        if (!StringUtils.hasText(request.getIp())) {
            return "ip is required";
        }
        if (request.getDeviceDetails() == null) {
            return "deviceDetails is required";
        }
        return null;
    }

    private boolean matchesRegisteredDevice(ApplicationUser applicationUser, String deviceId) {
        return applicationUser.getApplicationUserDeviceDetails() != null
                && StringUtils.hasText(applicationUser.getApplicationUserDeviceDetails().getDeviceId())
                && applicationUser.getApplicationUserDeviceDetails().getDeviceId().trim().equals(deviceId.trim());
    }

    @Transactional
    protected void updateApplicationUserDetails(ApplicationUser applicationUser) {
        try {
            log.info("Biometric login update user details {}", applicationUser.getUsername());
            applicationUser.setExpectingFirstTimeLogging(false);
            applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected ApplicationUserDetailsResponseDTO getUserProfileDetails(String username, BiometricLoginRequestDTO biometricLoginRequestDTO) {
        try {
            log.info("get user profile details by username biometric login time {}", username);
            ChannelRequestDTO channelRequestDTO = new ChannelRequestDTO();
            channelRequestDTO.setChannel(biometricLoginRequestDTO.getChannel());
            channelRequestDTO.setIp(biometricLoginRequestDTO.getIp());
            channelRequestDTO.setUsername(username);
            channelRequestDTO.setDeviceDetails(biometricLoginRequestDTO.getDeviceDetails());
            channelRequestDTO.setMessage(Messages.PROFILE_DETAILS.name());
            ResponseEntity<ApiResponse<Object>> profileDetailsResponse = authFeignClient.getProfileDetails(channelRequestDTO);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(profileDetailsResponse);
            ApplicationUserDetailsResponseDTO applicationUserDetailsResponseDTO = modelMapper.map(objectApiResponse, ApplicationUserDetailsResponseDTO.class);
            if (applicationUserDetailsResponseDTO.getUserPersonalDetails() != null
                    && applicationUserDetailsResponseDTO.getUserPersonalDetails().getUserCompanyDetails() != null
                    && applicationUserDetailsResponseDTO.getUserPersonalDetails().getUserCompanyDetails().getPreviousPermanentDate() == null) {
                applicationUserDetailsResponseDTO.getUserPersonalDetails().getUserCompanyDetails()
                        .setPreviousPermanentDate(applicationUserDetailsResponseDTO.getUserPersonalDetails().getUserCompanyDetails().getPermanentDate());
                applicationUserDetailsResponseDTO.getUserPersonalDetails().getUserCompanyDetails().setPermanentDate(null);
            }
            return applicationUserDetailsResponseDTO;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private AccessTokenResponseDTO issueToken(BiometricLoginRequestDTO biometricLoginRequestDTO, ChannelMbDeviceDetailsDTO deviceDetails) {
        ChannelRequestDTO channelRequestDTO = new ChannelRequestDTO();
        channelRequestDTO.setUsername(biometricLoginRequestDTO.getUsername());
        channelRequestDTO.setChannel(Channel.MB.name());
        channelRequestDTO.setMessage(Messages.SIGN_IN.name());
        channelRequestDTO.setIp(biometricLoginRequestDTO.getIp());
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
}
