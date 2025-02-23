/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:18 AM
 * <p>
 */

package com.dtech.auth.service.impl;


import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.MessageResponseDTO;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.enums.NotificationsType;
import com.dtech.auth.enums.Status;
import com.dtech.auth.feign.MessageFeignClient;
import com.dtech.auth.model.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    @Autowired
    private final OnboardingRequestRepository onboardingRequestRepository;

    @Autowired
    private final ApplicationPasswordHistoryRepository applicationPasswordHistoryRepository;

    @Autowired
    private final ApplicationUsernamePolicyRepository applicationUsernamePolicyRepository;

    @Autowired
    private final CompanyTypesRepository companyTypesRepository;

    @Autowired
    private final StaffCategoriesRepository staffCategoriesRepository;

    @Autowired
    private final StaffTypesRepository staffTypesRepository;

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
                    .map(userPersonalDetails -> applicationPasswordPolicyRepository.findPasswordPolicy()
                            .map(pw -> {
                                if (pw.getOnboardingOtpHistory() > 0) {
                                    log.info("Signup otp request policy - {}", pw.getOnboardingOtpHistory());

                                    Sort sort = Sort.by(Sort.Order.desc("createdDate"));
                                    List<OnboardingVerifiedMobile> onboardingVerifiedMobiles = onboardingVerifiedMobileRepository
                                            .findByEpfNoAndNicEqualsIgnoreCaseAndMobileAndVerified(userPersonalDetails.getEpfNo(), userPersonalDetails.getNic(),
                                                    signupOtpRequestDTO.getMobileNo().trim(), true, sort);

                                    LocalDateTime localDateTime = LocalDateTime.now().minusDays(pw.getOnboardingOtpHistory());
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
                            }))
                    .orElseGet(() -> {
                        log.info("Signup otp request user not found {}", signupOtpRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{signupOtpRequestDTO.getMobileNo()}, locale)));
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signupOtpValidation(OtpRequestDTO otpRequestDTO, Locale locale) {

        try {
            log.info("Processing SignupOtpValidation {}", otpRequestDTO);
            return applicationOtpSessionRepository.findByOtpAndValidated(otpRequestDTO.getOtp(), false).map(os -> onboardingVerifiedMobileRepository
                    .findByApplicationOtpSession(os).map(od -> {

                        if (DateTimeUtil.getSeconds(os.getCreatedDate(), 60).after(DateTimeUtil.getCurrentDateTime()) &&
                                os.getOtp().equals(otpRequestDTO.getOtp()) && !os.isValidated()) {
                            log.info("Otp request  for signup {} ", os);
                            updateOtpData(os, od);
                            return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.OTP_VALIDATION_SUCCESS, null, locale)));
                        }

                        log.info("Signup otp request validation fail otp or invalid session {}", os);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1016, messageSource.getMessage(ResponseMessageUtil.OTP_INVALID_OR_SESSION_TIME_OUT, null, locale)));
                    })
                    .orElseGet(() -> {
                        log.info("Signup otp mobile verified not found {}", otpRequestDTO);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1020, messageSource.getMessage(ResponseMessageUtil.ONBOARDING_VERIFICATION_OTP_NOT_FOUND, null, locale)));
                    })).orElseGet(() -> {
                log.info("Signup otp session not found {}", otpRequestDTO);
                return ResponseEntity.ok().body(responseUtil.error(null, 1015, messageSource.getMessage(ResponseMessageUtil.OTP_SESSION_NOT_FOUND, null, locale)));
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> signup(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO, Locale locale) {
        try {
            log.info("Processing Signup {}", userPersonalDetailsRequestDTO);

            String username = userPersonalDetailsRequestDTO.getUsername().trim();
            String password = userPersonalDetailsRequestDTO.getConfirmPassword().trim();

            //check username
            String alignUsername = validAlignCurrentUsernamePolicy(username);
            if (alignUsername == null || alignUsername.trim().isEmpty()) {
                log.info("username validation  success {}", username);
                boolean exists = applicationUserRepository
                        .existsByUsernameEndingWithIgnoreCase(userPersonalDetailsRequestDTO.getUsername().trim());

                if (!exists) {
                    //check password sta¤tus
                    String alignPassword = validAlignCurrentPasswordPolicy(password);
                    log.info("After signup password validation process {}", alignPassword);
                    if (alignPassword == null || alignPassword.trim().isEmpty()) {
                        
                        //check company details
                        String alignCompanyDetails = validCompanyDetails(userPersonalDetailsRequestDTO.getUserCompanyDetails());

                        if (alignCompanyDetails == null || alignCompanyDetails.trim().isEmpty()) {
                            return userPersonalDetailsRepository
                                    .findByEpfNoAndNicIgnoreCaseAndUserStatus(userPersonalDetailsRequestDTO.getEpfNo().trim(),
                                            userPersonalDetailsRequestDTO.getNic().trim(), Status.ACTIVE).map(pd -> {
                                        Optional<ApplicationUser> userPersonalDetails = applicationUserRepository.findByUserPersonalDetails(pd);

                                        if (userPersonalDetails.isPresent()) {
                                            log.info("User already sign up {}", userPersonalDetails.get());
                                            return ResponseEntity.ok().body(responseUtil.error(null, 1018, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_ALREADY_SIGN_UP, null, locale)));
                                        }

                                        String hashPassword = "";
                                        String saltKey = "";
                                        try {
                                            log.info("processing signup generate salt key {}", password);
                                            saltKey = PasswordUtil.generateSaltKey(
                                                    userPersonalDetailsRequestDTO.getNic().trim()
                                                            + DateTimeUtil.getCurrentDateTime());
                                        } catch (NoSuchAlgorithmException e) {
                                            log.error(e);
                                            throw new RuntimeException(e);
                                        }

                                        try {
                                            log.info("processing signup password hash {}", password);
                                            hashPassword = PasswordUtil.passwordEncoder(saltKey, password);
                                        } catch (NoSuchAlgorithmException e) {
                                            log.error(e);
                                            throw new RuntimeException(e);
                                        }
                                        OnboardingRequest onboardingRequest = updateOnboardingRequest(userPersonalDetailsRequestDTO);
                                        ApplicationUser applicationUser = updateApplicationUser(userPersonalDetailsRequestDTO, hashPassword, saltKey, onboardingRequest, pd);
                                        updateApplicationUserPasswordHistory(applicationUser, hashPassword);
                                        log.info("Signup register success {}", applicationUser);
                                        return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.SIGNUP_PROCESS_SUCCESS, null, locale)));

                                    }).orElseGet(() -> {
                                        log.info("Signup inquiry user not found {}", userPersonalDetailsRequestDTO);
                                        return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{clientMobile}, locale)));
                                    });
                        }
                        log.info("Signup not align company details {}", alignCompanyDetails);
                        return ResponseEntity.ok().body(responseUtil.error(null, 1022, alignCompanyDetails));
                    }
                    return ResponseEntity.ok().body(responseUtil.error(null, 1007, alignPassword));
                }
                log.info("Signup exists username {}", userPersonalDetailsRequestDTO.getUsername());
                return ResponseEntity.ok().body(responseUtil.error(null, 1021, messageSource.getMessage(ResponseMessageUtil.USERNAME_ALREADY_EXISTS, null, locale)));

            }
            log.info("Signup username not valid {}", username);
            return ResponseEntity.ok().body(responseUtil.error(null, 1020, alignUsername));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected ApplicationUser updateApplicationUser(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO,
                                                    String hashPassword, String saltKey,
                                                    OnboardingRequest onboardingRequest,
                                                    UserPersonalDetails userPersonalDetails) {
        try {
            log.info("Processing signup application user {}", userPersonalDetailsRequestDTO);
            ApplicationUser applicationUser = new ApplicationUser();

            applicationUser.setUsername(userPersonalDetailsRequestDTO.getUsername().trim());
            applicationUser.setPassword(hashPassword);
            applicationUser.setUserKey(saltKey);
            applicationUser.setPrimaryEmail(userPersonalDetailsRequestDTO.getEmail().trim());
            applicationUser.setPrimaryMobile(userPersonalDetailsRequestDTO.getMobileNo().trim());
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setReset(false);
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setAttemptCount(0);
            applicationUser.setOtpAttemptCount(0);
            applicationUser.setExpectingFirstTimeLogging(true);
            applicationUser.setMbExpectingFirstTimeLogging(true);
            applicationUser.setExpectingDependentsRegister(true);
            applicationUser.setOnboardingRequest(onboardingRequest);
            applicationUser.setUserPersonalDetails(userPersonalDetails);
            log.info(" Signup Application user {} updated", userPersonalDetailsRequestDTO);
            return applicationUserRepository.saveAndFlush(applicationUser);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Transactional
    protected OnboardingRequest updateOnboardingRequest(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO) {
        try {
            log.info("Processing signup updateOnboardingRequest {}", userPersonalDetailsRequestDTO);
            OnboardingRequest onboardingRequest = new OnboardingRequest();
            onboardingRequest.setRequestStatus(Status.COMPLETED);
            onboardingRequest.setUserCustomDetails(userPersonalDetailsRequestDTO.toString());
            return onboardingRequestRepository.saveAndFlush(onboardingRequest);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }

    }

    @Transactional
    protected void updateApplicationUserPasswordHistory(ApplicationUser applicationUser, String hashPassword) {
        try {
            log.info("Updating application user password history for reset password {}", applicationUser);
            ApplicationPasswordHistory applicationPasswordHistory = new ApplicationPasswordHistory();
            applicationPasswordHistory.setApplicationUser(applicationUser);
            applicationPasswordHistory.setPassword(hashPassword);
            applicationPasswordHistoryRepository.saveAndFlush(applicationPasswordHistory);
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected String validAlignCurrentUsernamePolicy(String username) {
        try {
            log.info("Signup username align with current username policy {}", username);
            return applicationUsernamePolicyRepository.findUsernamePolicy()
                    .map(policy -> {
                        int charCount = StringUtil.getCharCount(username);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("Signup username invalid max length validation {}", username);
                            return messageSource.getMessage("val.username.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("Signup username invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.username.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = StringUtil.countCharsByConditions(username, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("Signup username invalid upper case validation {}", username);
                            return messageSource.getMessage("val.username.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = StringUtil.countCharsByConditions(username, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("Signup username invalid lower case validation {}", username);
                            return messageSource.getMessage("val.username.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = StringUtil.countCharsByConditions(username, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("Signup username invalid min digit validation {}", username);
                            return messageSource.getMessage("val.username.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = StringUtil.countCharsByConditions(username, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("Signup username invalid special char validation {}", username);
                            return messageSource.getMessage("val.username.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }
                        log.info("Signup username success validation {}", username);
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("Signup request username policy not found for username {} ", username);
                        return messageSource.getMessage("val.username.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected String validAlignCurrentPasswordPolicy(String password) {
        try {
            log.info("Signup password align with current password policy {}", password);
            return applicationPasswordPolicyRepository.findPasswordPolicy()
                    .map(policy -> {
                        int charCount = StringUtil.getCharCount(password);
                        // max length check
                        if (charCount > policy.getMaxLength()) {
                            log.info("Signup password invalid max length validation {}", password);
                            return messageSource.getMessage("val.password.max.length.invalid", new Object[]{policy.getMaxLength()}, null);
                        }

                        // min length check
                        if (charCount < policy.getMinLength()) {
                            log.info("Signup password invalid min length validation char count {} policy min length {}", charCount, policy.getMinLength());
                            return messageSource.getMessage("val.password.min.length.invalid", new Object[]{policy.getMinLength()}, null);
                        }

                        int upperCount = StringUtil.countCharsByConditions(password, Character::isUpperCase);
                        // upper count check
                        if (upperCount < policy.getMinUpperCase()) {
                            log.info("Signup password invalid upper case validation {}", password);
                            return messageSource.getMessage("val.password.upper.length.invalid", new Object[]{policy.getMinUpperCase()}, null);
                        }

                        int lowerCount = StringUtil.countCharsByConditions(password, Character::isLowerCase);
                        // lower count check
                        if (lowerCount < policy.getMinLowerCase()) {
                            log.info("Signup password invalid lower case validation {}", password);
                            return messageSource.getMessage("val.password.lower.length.invalid", new Object[]{policy.getMinLowerCase()}, null);
                        }

                        int digitCount = StringUtil.countCharsByConditions(password, Character::isDigit);
                        // number count check
                        if (digitCount < policy.getMinNumbers()) {
                            log.info("Signup password invalid min digit validation {}", password);
                            return messageSource.getMessage("val.password.number.length.invalid", new Object[]{policy.getMinNumbers()}, null);
                        }

                        int specialCharCount = StringUtil.countCharsByConditions(password, c -> !Character.isLetterOrDigit(c));
                        // special char count check
                        if (specialCharCount < policy.getMinSpecialCharacters()) {
                            log.info("Signup password invalid special char validation {}", password);
                            return messageSource.getMessage("val.password.special.length.invalid", new Object[]{policy.getMinSpecialCharacters()}, null);
                        }
                        log.info("Signup password success validation {}", password);
                        return "";
                    })
                    .orElseGet(() -> {
                        log.info("Signup request policy not found for username {} ", password);
                        return messageSource.getMessage("val.password.policy.notfound", null, null);
                    });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected String validCompanyDetails(UserCompanyDetailsRequestDTO userCompanyDetailsRequestDTO) {
        try {
            log.info("Signup company details {}", userCompanyDetailsRequestDTO);

            //company type
            Optional<CompanyTypes> companyTypes = companyTypesRepository.findByCodeAndStatus(userCompanyDetailsRequestDTO
                    .getCompanyType().getCode(), Status.ACTIVE);

            //staff category
            Optional<StaffCategories> staffCategories = staffCategoriesRepository.findByCodeAndStatus(userCompanyDetailsRequestDTO
                    .getStaffCategory().getCode(), Status.ACTIVE);

            //staff type
            Optional<StaffTypes> staffTypes = staffTypesRepository.findByCodeAndStatus(userCompanyDetailsRequestDTO
                    .getStaffType().getCode(), Status.ACTIVE);

            if (companyTypes.isEmpty()) {
                log.info("Signup company types not found {}", userCompanyDetailsRequestDTO.getCompanyType());
                return messageSource.getMessage("val.company.types.notfound", null, null);
            } else if (staffCategories.isEmpty()) {
                log.info("Signup staff category types not found {}", userCompanyDetailsRequestDTO.getStaffCategory());
                return messageSource.getMessage("val.staff.category.notfound", null, null);
            } else if (staffTypes.isEmpty()) {
                log.info("Signup staff types not found {}", userCompanyDetailsRequestDTO.getStaffType());
                return messageSource.getMessage("val.staff.type.notfound", null, null);
            }
            return "";
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    protected void updateOtpData(ApplicationOtpSession applicationOtpSession, OnboardingVerifiedMobile onboardingVerifiedMobile) {
        log.info("Update sign up otp validation request otp records");
        applicationOtpSession.setValidated(true);
        onboardingVerifiedMobile.setVerified(true);
        applicationOtpSessionRepository.saveAndFlush(applicationOtpSession);
        onboardingVerifiedMobileRepository.saveAndFlush(onboardingVerifiedMobile);
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
            if (objectApiResponse != null) {
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
