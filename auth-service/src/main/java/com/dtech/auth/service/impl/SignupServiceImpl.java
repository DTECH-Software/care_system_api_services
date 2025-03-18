/**
 * User: Himal_J
 * Date: 2/18/2025
 * Time: 9:18 AM
 * <p>
 */

package com.dtech.auth.service.impl;


import com.dtech.auth.dto.SimpleBaseDTO;
import com.dtech.auth.dto.request.*;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.MessageResponseDTO;
import com.dtech.auth.dto.response.PolicyResponseDTO;
import com.dtech.auth.dto.response.UserPersonalDetailsResponseDTO;
import com.dtech.auth.enums.*;
import com.dtech.auth.feign.MessageFeignClient;
import com.dtech.auth.model.*;
import com.dtech.auth.repository.*;
import com.dtech.auth.service.SignupService;
import com.dtech.auth.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.dtech.auth.util.EnumUtil.getEnumList;

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

    @Autowired
    private final ObjectMapper objectMapper;

    @Autowired
    private final MarriedRepository marriedRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> splash(ChannelRequestDTO channelRequestDTO, Locale locale) {

        try {
            log.info("Splash Request {} ", channelRequestDTO);
            Map<String, Object> splashData = new HashMap<>();
            List<SimpleBaseDTO> marriedRounds = marriedRepository.findAllByStatus(Status.ACTIVE)
                    .stream()
                    .map(val -> new SimpleBaseDTO(val.getCode(), val.getDescription()))
                    .toList();
            splashData.put("marriedRounds", marriedRounds);
            splashData.put("passwordPolicy", modelMapper.map(applicationPasswordPolicyRepository.findPasswordPolicy().orElse(null),PolicyResponseDTO.class));
            splashData.put("usernamePolicy", modelMapper.map(applicationUsernamePolicyRepository.findUsernamePolicy().orElse(null),PolicyResponseDTO.class));
            splashData.put("gender", getEnumList(Gender.class));
            splashData.put("dependents", getEnumList(DependentCategory.class));
            splashData.put("title", getEnumList(Title.class));
            splashData.put("docTypes", getEnumList(DocType.class));
            splashData.put("relationCategories", getEnumList(RelationCategory.class));
            log.info("Splash request success{} ", channelRequestDTO);
            return ResponseEntity.ok().body(responseUtil.success(splashData, messageSource.getMessage(ResponseMessageUtil.SPLASH_SUCCESS, null, locale)));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

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
                        log.info("Sign up inquiry end {} ",userPersonalDetailsResponseDTO);
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
                        .existsByUsernameEqualsIgnoreCase(userPersonalDetailsRequestDTO.getUsername().trim());

                boolean existsMobile = applicationUserRepository
                        .existsByPrimaryMobileAndUserPersonalDetails_UserStatus(userPersonalDetailsRequestDTO.getMobileNo().trim(), Status.ACTIVE);

                boolean existsEmail = applicationUserRepository
                        .existsByPrimaryEmailIgnoreCase(userPersonalDetailsRequestDTO.getEmail().trim());

                if (exists) {
                    log.info("Signup exists username {}", userPersonalDetailsRequestDTO.getUsername());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1021, messageSource.getMessage(ResponseMessageUtil.USERNAME_ALREADY_EXISTS, null, locale)));
                } else if (existsMobile) {
                    log.info("Signup exists mobile {}", userPersonalDetailsRequestDTO.getMobileNo());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1025, messageSource.getMessage(ResponseMessageUtil.PRIMARY_MOBILE_ALREADY_IN_USE, null, locale)));
                } else if (existsEmail) {
                    log.info("Signup exists email {}", userPersonalDetailsRequestDTO.getMobileNo());
                    return ResponseEntity.ok().body(responseUtil.error(null, 1026, messageSource.getMessage(ResponseMessageUtil.PRIMARY_EMAIL_ALREADY_IN_USE, null, locale)));
                }

                //check password sta¤tus
                String alignPassword = validAlignCurrentPasswordPolicy(password);
                log.info("After signup password validation process {}", alignPassword);
                if (alignPassword == null || alignPassword.trim().isEmpty()) {

                    //check company details
             //       String alignCompanyDetails = validCompanyDetails(userPersonalDetailsRequestDTO.getUserCompanyDetails());

              //      if (alignCompanyDetails == null || alignCompanyDetails.trim().isEmpty()) {
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
                                    String jsonString = "";
                                    try {
                                        log.info("processing signup  json {}", userPersonalDetailsRequestDTO);
                                        jsonString = objectMapper.writeValueAsString(userPersonalDetailsRequestDTO);
                                    } catch (JsonProcessingException e) {
                                        log.error(e);
                                        throw new RuntimeException(e);
                                    }
                                    OnboardingRequest onboardingRequest = updateOnboardingRequest(userPersonalDetailsRequestDTO, jsonString);
                                    ApplicationUser applicationUser = updateApplicationUser(userPersonalDetailsRequestDTO, hashPassword, saltKey, onboardingRequest, pd);
                                    updateApplicationUserPasswordHistory(applicationUser, hashPassword);
                                    log.info("Signup register success {}", applicationUser);
                                    return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.SIGNUP_PROCESS_SUCCESS, null, locale)));

                                }).orElseGet(() -> {
                                    log.info("Signup inquiry user not found {}", userPersonalDetailsRequestDTO);
                                    return ResponseEntity.ok().body(responseUtil.error(null, 1017, messageSource.getMessage(ResponseMessageUtil.EMPLOYEE_DETAILS_NOT_FOUND_ON_SYSTEM, new Object[]{clientMobile}, locale)));
                                });
//                    }
//                    log.info("Signup not align company details {}", alignCompanyDetails);
//                    return ResponseEntity.ok().body(responseUtil.error(null, 1022, alignCompanyDetails));
                }
                return ResponseEntity.ok().body(responseUtil.error(null, 1007, alignPassword));


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
            applicationUser.setPrimaryEmail(userPersonalDetailsRequestDTO.getEmail().trim().toLowerCase());
            applicationUser.setPrimaryMobile(userPersonalDetailsRequestDTO.getMobileNo().trim());
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setReset(false);
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setAttemptCount(0);
            applicationUser.setOtpAttemptCount(0);
            applicationUser.setExpectingFirstTimeLogging(true);
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
    protected OnboardingRequest updateOnboardingRequest(UserPersonalDetailsRequestDTO userPersonalDetailsRequestDTO, String jsonString) {
        try {
            log.info("Processing signup updateOnboardingRequest {}", userPersonalDetailsRequestDTO);
            OnboardingRequest onboardingRequest = new OnboardingRequest();
            onboardingRequest.setRequestStatus(Status.ACTIVE);
            onboardingRequest.setUserCustomDetails(jsonString);
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
                    .getCompanyTypes().getCode(), Status.ACTIVE);

            //staff category
            Optional<StaffCategories> staffCategories = staffCategoriesRepository.findByCodeAndStatus(userCompanyDetailsRequestDTO
                    .getStaffCategories().getCode(), Status.ACTIVE);

            //staff type
            Optional<StaffTypes> staffTypes = staffTypesRepository.findByCodeAndStatus(userCompanyDetailsRequestDTO
                    .getStaffTypes().getCode(), Status.ACTIVE);

            if (companyTypes.isEmpty()) {
                log.info("Signup company types not found {}", userCompanyDetailsRequestDTO.getCompanyTypes());
                return messageSource.getMessage("val.company.types.notfound", null, null);
            } else if (staffCategories.isEmpty()) {
                log.info("Signup staff category types not found {}", userCompanyDetailsRequestDTO.getStaffCategories());
                return messageSource.getMessage("val.staff.category.notfound", null, null);
            } else if (staffTypes.isEmpty()) {
                log.info("Signup staff types not found {}", userCompanyDetailsRequestDTO.getStaffTypes());
                return messageSource.getMessage("val.staff.type.notfound", null, null);
            }
            return "";
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
            messageRequestDTO.setType(NotificationsType.OTP.name());

            log.info("Before token request mapper {} ", messageRequestDTO);
            log.info("Before calling message service {}", messageFeignClient);
            ResponseEntity<ApiResponse<Object>> messageResponse = messageFeignClient.sendMessage(messageRequestDTO);
            log.info("After response message service {}", messageResponse);
            Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(messageResponse);
            log.info("After message mapper response {}", objectApiResponse);
            MessageResponseDTO messageResponseDTO = gson.fromJson(gson.toJson(objectApiResponse), MessageResponseDTO.class);
            log.info("Otp send status {}", messageResponseDTO);
            ApplicationOtpSession applicationOtpSession = updateOtpSession(otp, messageResponseDTO != null ? messageResponseDTO.isSuccess() : false);
            updateOnboardingVerifiedMobile(signupOtpRequestDTO, applicationOtpSession);
            log.info("Application OTP session updated successfully - onboarding verified mobile {}", otp);
            if (objectApiResponse != null) {
                return ResponseEntity.ok().body(responseUtil.success(null, messageSource.getMessage(ResponseMessageUtil.OTP_SEND_SUCCESS, null, locale)));
            }
            return ResponseEntity.ok().body(responseUtil.error(null, 1019, messageSource.getMessage(ResponseMessageUtil.OTP_SEND_FAILED, null, locale)));
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
    protected ApplicationOtpSession updateOtpSession(String otp, boolean state) {
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
            userPersonalDetailsResponseDTO.setGenderDescription(Gender.valueOf(userPersonalDetailsResponseDTO.getGender()).getDescription());
            userPersonalDetailsResponseDTO.setTitleDescription(Title.valueOf(userPersonalDetailsResponseDTO.getTitle()).getDescription());
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
