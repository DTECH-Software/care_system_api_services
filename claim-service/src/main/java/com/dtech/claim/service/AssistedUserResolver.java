package com.dtech.claim.service;

import com.dtech.claim.dto.request.ChannelRequestDTO;
import com.dtech.claim.dto.request.validator.ChannelRequestValidatorDTO;
import com.dtech.claim.enums.Status;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.Document;
import com.dtech.claim.model.OnboardingRequest;
import com.dtech.claim.model.UserPersonalDetails;
import com.dtech.claim.repository.ApplicationUserRepository;
import com.dtech.claim.repository.OnboardingRequestRepository;
import com.dtech.claim.repository.UserPersonalDetailsRepository;
import com.dtech.claim.util.DateTimeUtil;
import com.dtech.claim.util.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AssistedUserResolver {
    private final ApplicationUserRepository applicationUserRepository;
    private final UserPersonalDetailsRepository userPersonalDetailsRepository;
    private final OnboardingRequestRepository onboardingRequestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<ApplicationUser> resolve(ChannelRequestDTO request) {
        if (Boolean.TRUE.equals(request.getAssistedMode())) {
            return resolveAssisted(request.getUsername(), request.getActingEmployeeId());
        }
        if (request.getUsername() == null) {
            return Optional.empty();
        }
        return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(request.getUsername().trim(), Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationUser> resolve(ChannelRequestValidatorDTO request) {
        if (Boolean.TRUE.equals(request.getAssistedMode())) {
            return resolveAssisted(request.getUsername(), request.getActingEmployeeId());
        }
        if (request.getUsername() == null) {
            return Optional.empty();
        }
        return applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(request.getUsername().trim(), Status.ACTIVE);
    }

    @Transactional
    public Optional<ApplicationUser> selectEmployee(String hrUsername, String epfNo) {
        if (!isHrUser(hrUsername)) {
            return Optional.empty();
        }
        List<UserPersonalDetails> employees = userPersonalDetailsRepository.findByEpfNoAndUserStatus(epfNo.trim(), Status.ACTIVE)
                .stream()
                .filter(employee -> hasCompanyAccess(hrUsername, getCompanyCode(employee)))
                .toList();
        if (employees.size() != 1) {
            log.info("Assisted employee select expected one active employee for epf {}, found {}", epfNo, employees.size());
            return Optional.empty();
        }
        ApplicationUser applicationUser = ensureApplicationUser(employees.get(0), hrUsername);
        initializeForResponse(applicationUser);
        return Optional.of(applicationUser);
    }

    private Optional<ApplicationUser> resolveAssisted(String username, Long actingEmployeeId) {
        if (actingEmployeeId == null || !isHrUser(username)) {
            return Optional.empty();
        }
        return applicationUserRepository.findByIdAndUserPersonalDetails_UserStatus(actingEmployeeId, Status.ACTIVE)
                .filter(user -> hasCompanyAccess(username, getCompanyCode(user.getUserPersonalDetails())));
    }

    private ApplicationUser ensureApplicationUser(UserPersonalDetails userPersonalDetails, String hrUsername) {
        return applicationUserRepository.findByUserPersonalDetails(userPersonalDetails)
                .orElseGet(() -> createApplicationUser(userPersonalDetails, hrUsername));
    }

    private void initializeForResponse(ApplicationUser applicationUser) {
        UserPersonalDetails personalDetails = applicationUser.getUserPersonalDetails();
        if (personalDetails == null) {
            return;
        }
        touchDocument(applicationUser.getProfileImg());
        touchDocument(personalDetails.getBirthImg());
        if (personalDetails.getUserAddress() != null) {
            personalDetails.getUserAddress().getCity();
        }
        if (personalDetails.getUserCompanyDetails() != null) {
            personalDetails.getUserCompanyDetails().getDesignation();
            if (personalDetails.getUserCompanyDetails().getCompanyTypes() != null) {
                personalDetails.getUserCompanyDetails().getCompanyTypes().getDescription();
            }
            if (personalDetails.getUserCompanyDetails().getStaffCategories() != null) {
                personalDetails.getUserCompanyDetails().getStaffCategories().getDescription();
            }
            if (personalDetails.getUserCompanyDetails().getStaffTypes() != null) {
                personalDetails.getUserCompanyDetails().getStaffTypes().getDescription();
            }
            if (personalDetails.getUserCompanyDetails().getInsurancePolicy() != null) {
                personalDetails.getUserCompanyDetails().getInsurancePolicy().getDescription();
            }
        }
    }

    private void touchDocument(Document document) {
        if (document != null) {
            document.getFileName();
            document.getFileType();
            document.getDoc();
        }
    }

    private ApplicationUser createApplicationUser(UserPersonalDetails personalDetails, String hrUsername) {
        try {
            OnboardingRequest onboardingRequest = new OnboardingRequest();
            onboardingRequest.setRequestStatus(Status.ACTIVE);
            onboardingRequest.setUserCustomDetails("{}");
            onboardingRequest = onboardingRequestRepository.saveAndFlush(onboardingRequest);

            String username = buildAssistedUsername(personalDetails);
            String saltKey = PasswordUtil.generateSaltKey(username + UUID.randomUUID());
            String temporaryPassword = UUID.randomUUID().toString();

            ApplicationUser applicationUser = new ApplicationUser();
            applicationUser.setFacilityId(generateFacilityId());
            applicationUser.setUsername(username);
            applicationUser.setPrimaryEmail(personalDetails.getEmail());
            applicationUser.setPrimaryMobile(personalDetails.getMobileNo());
            applicationUser.setUserKey(saltKey);
            applicationUser.setPassword(PasswordUtil.passwordEncoder(saltKey, temporaryPassword));
            applicationUser.setLoginStatus(Status.ACTIVE);
            applicationUser.setReset(true);
            applicationUser.setExpectingFirstTimeLogging(true);
            applicationUser.setExpectingDependentsRegister(true);
            applicationUser.setPasswordExpiredDate(DateTimeUtil.get30FutureDate());
            applicationUser.setLastLoggedDate(DateTimeUtil.getCurrentDateTime());
            applicationUser.setAttemptCount(0);
            applicationUser.setOtpAttemptCount(0);
            applicationUser.setOtpAttemptResetTime(DateTimeUtil.getCurrentDateTime());
            applicationUser.setOnboardingRequest(onboardingRequest);
            applicationUser.setUserPersonalDetails(personalDetails);
            log.info("Creating assisted application user for epf {} by {}", personalDetails.getEpfNo(), hrUsername);
            return applicationUserRepository.saveAndFlush(applicationUser);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create assisted application user", e);
        }
    }

    private String buildAssistedUsername(UserPersonalDetails personalDetails) {
        String email = personalDetails.getEmail() == null ? "" : personalDetails.getEmail().trim();
        if (!email.isBlank() && !applicationUserRepository.existsByUsernameEqualsIgnoreCase(email)) {
            return email;
        }
        String base = "EPF" + personalDetails.getEpfNo();
        String candidate = base;
        int suffix = 1;
        while (applicationUserRepository.existsByUsernameEqualsIgnoreCase(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private String generateFacilityId() {
        Number maxId = (Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) FROM application_user")
                .getSingleResult();
        return String.format("%04d", maxId.longValue());
    }

    private boolean isHrUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM web_user wu
                JOIN web_user_role wur ON wu.user_role = wur.code
                WHERE wu.username = :username
                  AND wu.status = 'ACTIVE'
                  AND wu.login_status = 'ACTIVE'
                  AND UPPER(wur.code) IN ('HRADMIN', 'HR', 'SUPERADMIN')
                """)
                .setParameter("username", username.trim())
                .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean hasCompanyAccess(String username, String companyCode) {
        if (username == null || companyCode == null) {
            return false;
        }
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM web_user wu
                WHERE wu.username = :username
                  AND wu.status = 'ACTIVE'
                  AND wu.login_status = 'ACTIVE'
                  AND (
                      NOT EXISTS (SELECT 1 FROM web_user_company wuc_all WHERE wuc_all.web_user_id = wu.id)
                      OR EXISTS (
                          SELECT 1
                          FROM web_user_company wuc
                          JOIN company_types ct ON ct.id = wuc.company_id
                          WHERE wuc.web_user_id = wu.id
                            AND ct.code = :companyCode
                      )
                  )
                """)
                .setParameter("username", username.trim())
                .setParameter("companyCode", companyCode)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private String getCompanyCode(UserPersonalDetails userPersonalDetails) {
        if (userPersonalDetails == null
                || userPersonalDetails.getUserCompanyDetails() == null
                || userPersonalDetails.getUserCompanyDetails().getCompanyTypes() == null) {
            return null;
        }
        return userPersonalDetails.getUserCompanyDetails().getCompanyTypes().getCode();
    }
}
