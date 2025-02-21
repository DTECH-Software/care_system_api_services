/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:18 AM
 * <p>
 */

package com.dtech.auth.service.impl;


import com.dtech.auth.dto.request.MessageRequestDTO;
import com.dtech.auth.dto.request.SignupInquiryDTO;
import com.dtech.auth.dto.request.SignupOtpRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.MessageResponseDTO;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.enums.NotificationsType;
import com.dtech.auth.enums.Status;
import com.dtech.auth.feign.MessageFeignClient;
import com.dtech.auth.model.ApplicationOtpSession;
import com.dtech.auth.model.OnboardingVerifiedMobile;
import com.dtech.auth.repository.*;
import com.dtech.auth.service.SignupService;
import com.dtech.auth.util.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    @Autowired
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;

    @Autowired
    private final ApplicationUserRepository applicationUserRepository;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Value("${client.mobile}")
    private String clientMobile;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final OnboardingVerifiedMobileRepository onboardingVerifiedMobileRepository;

    @Autowired
    private final ApplicationPasswordPolicyRepository applicationPasswordPolicyRepository;

    @Autowired
    private final ApplicationOtpSessionRepository applicationOtpSessionRepository;

    @Autowired
    private final Gson gson;

    @Autowired
    private final MessageFeignClient messageFeignClient;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signupInquiry(SignupInquiryDTO signupInquiryDTO, Locale locale) {
        try {
            log.info("Processing SignupInquiry {}", signupInquiryDTO);

            return userPersonalDetailsRepository.findByEpfNoAndNicIgnoreCaseAndUserStatus(signupInquiryDTO.getEpfNo().trim(), signupInquiryDTO.getNic().trim(), Status.ACTIVE)
                    .map(user -> applicationUserRepository.findByUserPersonalDetails(user).map(applicationUser -> {
                        log.info("User already sign up {}", applicationUser);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1018, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_ALREADY_SIGN_UP, null, locale)));
                    }).orElseGet(() -> {
                        log.info("Sign up inquiry start");
                        UserPersonalDetailsResponseDTO userPersonalDetailsResponseDTO = modelMapper.map(user, UserPersonalDetailsResponseDTO.class);
                        getAge(userPersonalDetailsResponseDTO);
                        log.info("Sign up inquiry end");
                        return ResponseEntity.ok().body(responseUtil.success(userPersonalDetailsResponseDTO, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_INQUIRY_SUCCESS, null, locale)));
                    }))
                    .orElseGet(() -> {
                        log.info("Signup inquiry user not found {}", signupInquiryDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{clientMobile}, locale)));
                    });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signupOtpRequest(SignupOtpRequestDTO signupOtpRequestDTO, Locale locale) {
        try {
            log.info("Processing SignupOtpRequest {}", signupOtpRequestDTO);
            return userPersonalDetailsRepository.findByEpfNoAndNicIgnoreCaseAndUserStatus(signupOtpRequestDTO.getEpfNo().trim(), signupOtpRequestDTO.getNic().trim(), Status.ACTIVE)
                    .map(userPersonalDetails -> {
                        return applicationPasswordPolicyRepository.findPasswordPolicy()
                                .map(pw -> {
                                    if (pw.getOnboardingOtpHistory() > 0) {
                                        log.info("Signup otp request policy - {}", pw.getOnboardingOtpHistory());

                                        Sort sort =  Sort.by(Sort.Order.desc("createdDate"));
                                        List<OnboardingVerifiedMobile> onboardingVerifiedMobiles = onboardingVerifiedMobileRepository
                                                .findByEpfNoAndNicEqualsIgnoreCaseAndMobileAndVerified(userPersonalDetails.getEpfNo(), userPersonalDetails.getNic(),
                                                signupOtpRequestDTO.getMobileNo().trim(),true, sort);

                                        LocalDateTime localDateTime = LocalDateTime.now().minusDays(pw.getOnboardingOtpHistory());
                                        System.out.println(localDateTime);
                                        boolean history = onboardingVerifiedMobiles.stream().anyMatch((verifiedMobile) -> verifiedMobile.getCreatedDate().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(localDateTime));

                                        if (history) {
                                            log.info("Sign up otp already verified");
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1018, messageSource.getMessage(ResponseMessageUtil.OTP_ALREADY_VERIFIED, null, locale)));
                                        }
                                    }
                                    log.info("Signup otp request success");
                                    return sendMessage(signupOtpRequestDTO, locale);
                                })
                                .orElseGet(() -> {
                                    log.info("Signup otp request password policy not found {}", signupOtpRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1010, messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_PASSWORD_POLICY_NOT_FOUND, null, locale)));
                                });

                    })
                    .orElseGet(() -> {
                        log.info("Signup otp request user not found {}", signupOtpRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{signupOtpRequestDTO.getMobileNo()}, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ResponseEntity<ApiResponse<Object>> sendMessage(SignupOtpRequestDTO signupOtpRequestDTO, Locale locale) {
        try {
            log.info("Processing onboarding otp request gen otp {} ", signupOtpRequestDTO);
            String otp = RandomGeneratorUtil.getRandom6DigitNumber();
            log.info("Generate otp - onboarding verified {} ", otp);
            MessageRequestDTO messageRequestDTO = new MessageRequestDTO();
            messageRequestDTO.setValue(otp);
            messageRequestDTO.setMobileNo(signupOtpRequestDTO.getMobileNo());
            messageRequestDTO.setType(NotificationsType.ONBOARDING_OTP.name());

            log.info("Before token request mapper {} ", messageRequestDTO);
            log.info("Before calling message service {}", messageFeignClient);
            ResponseEntity<ApiResponse<Object>> messageResponse = messageFeignClient.sendMessage(messageRequestDTO);
            log.info("After response message service {}", messageResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(messageResponse);
            log.info("After message mapper response {}", objectApiResponse);
            MessageResponseDTO messageResponseDTO = gson.fromJson(gson.toJson(objectApiResponse), MessageResponseDTO.class);
            log.info("Otp send status {}", messageResponseDTO);
            ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO != null ? messageResponseDTO.getSuccess() : 0);
            updateOnboardingVerifiedMobile(signupOtpRequestDTO, applicationOtpSession);
            log.info("Application OTP session updated successfully - onboarding verified mobile {}", otp);
            if(objectApiResponse != null) {
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.OTP_SEND_SUCCESS, null, locale)));
            }
            return ResponseEntity.ok().body(responseUtil.error(null, 1019, messageSource.getMessage(ResponseMessageUtil.OTP_SENT_FAILED, null, locale)));
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateOnboardingVerifiedMobile(SignupOtpRequestDTO signupOtpRequestDTO, ApplicationOtpSession applicationOtpSession) {
        try {
            log.info("Update record updateOnboardingVerifiedMobile {}", signupOtpRequestDTO);
            OnboardingVerifiedMobile onboardingVerifiedMobile = new OnboardingVerifiedMobile();
            onboardingVerifiedMobile.setNic(signupOtpRequestDTO.getNic().trim());
            onboardingVerifiedMobile.setEpfNo(signupOtpRequestDTO.getEpfNo().trim());
            onboardingVerifiedMobile.setMobile(signupOtpRequestDTO.getMobileNo().trim());
            onboardingVerifiedMobile.setVerified(false);
            onboardingVerifiedMobile.setApplicationOtpSession(applicationOtpSession);
            onboardingVerifiedMobileRepository.saveAndFlush(onboardingVerifiedMobile);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApplicationOtpSession updateOtpSession(String otp, int state) {
        try {
            log.info("Processing onboarding otp request  application otp session update {} ", otp);
            ApplicationOtpSession applicationOtpSession = new ApplicationOtpSession();
            applicationOtpSession.setOtp(otp);
            applicationOtpSession.setSuccess(state);
            ApplicationOtpSession otpSession = applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
            log.info("Processing onboarding otp request otp session update {} ", otpSession);
            return otpSession;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected void getAge(UserPersonalDetailsResponseDTO userPersonalDetailsResponseDTO) {
        try {
            log.info("Processing getAge {}", userPersonalDetailsResponseDTO);
            userPersonalDetailsResponseDTO.setAge(DateTimeUtil.getAge(
                    String.valueOf(userPersonalDetailsResponseDTO.getDob())));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
