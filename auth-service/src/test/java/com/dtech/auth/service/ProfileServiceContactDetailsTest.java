package com.dtech.auth.service;

import com.dtech.auth.dto.request.ProfileEditRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.enums.Status;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.model.ApplicationOtpSession;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.*;
import com.dtech.auth.service.impl.ProfileServiceImpl;
import com.dtech.auth.util.ResponseUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceContactDetailsTest {

    private ApplicationUserRepository applicationUserRepository;
    private ApplicationOtpSessionRepository applicationOtpSessionRepository;
    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        applicationUserRepository = mock(ApplicationUserRepository.class);
        applicationOtpSessionRepository = mock(ApplicationOtpSessionRepository.class);
        service = new ProfileServiceImpl(
                applicationUserRepository,
                mock(MessageSource.class),
                new ResponseUtil(),
                mock(ClaimDependentsRepository.class),
                new Gson(),
                mock(AssistedUserResolver.class),
                mock(DocumentFeignClient.class),
                mock(MarriedRepository.class),
                mock(NotificationHistoryRepository.class),
                mock(DocumentStoreRepository.class),
                mock(EmailNotificationService.class),
                mock(DependentEmailRecipientConfigService.class),
                applicationOtpSessionRepository,
                mock(MaritalStatusRepository.class));
        ReflectionTestUtils.setField(service, "otpValiditySeconds", 300);
    }

    @Test
    void updateByEmailChangesApplicationUserAndPersonalDetailsTogether() {
        ApplicationUser user = activeUser();
        ApplicationOtpSession session = validatedSession(user.getId());
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(
                "old@example.com", Status.ACTIVE)).thenReturn(Optional.empty());
        when(applicationUserRepository.findByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatus(
                "old@example.com", Status.ACTIVE)).thenReturn(Optional.of(user));
        when(applicationOtpSessionRepository.findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(
                user.getId(), "PROFILE_UPDATE")).thenReturn(Optional.of(session));

        ResponseEntity<ApiResponse<Object>> response = service.updateProfileDetails(
                request("old@example.com", "new@example.com", "0761234567"), Locale.ENGLISH);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("new@example.com", user.getPrimaryEmail());
        assertEquals("0761234567", user.getPrimaryMobile());
        assertEquals("new@example.com", user.getUserPersonalDetails().getEmail());
        assertEquals("0761234567", user.getUserPersonalDetails().getMobileNo());
        assertTrue(session.isConsumed());
        verify(applicationUserRepository, atLeastOnce()).saveAndFlush(user);
        verify(applicationOtpSessionRepository).saveAndFlush(session);
    }

    @Test
    void duplicateEmailIsRejectedBeforeOtpIsConsumed() {
        ApplicationUser user = activeUser();
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(
                "employee", Status.ACTIVE)).thenReturn(Optional.of(user));
        when(applicationUserRepository.existsByPrimaryEmailIgnoreCaseAndUserPersonalDetails_UserStatusAndIdNot(
                "used@example.com", Status.ACTIVE, user.getId())).thenReturn(true);

        ResponseEntity<ApiResponse<Object>> response = service.updateProfileDetails(
                request("employee", "used@example.com", user.getPrimaryMobile()), Locale.ENGLISH);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(1026, response.getBody().getErrorCode());
        verifyNoInteractions(applicationOtpSessionRepository);
        verify(applicationUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidOtpCannotUpdateProfile() {
        ApplicationUser user = activeUser();
        ApplicationOtpSession session = validatedSession(user.getId());
        session.setOtp("654321");
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(
                "employee", Status.ACTIVE)).thenReturn(Optional.of(user));
        when(applicationOtpSessionRepository.findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(
                user.getId(), "PROFILE_UPDATE")).thenReturn(Optional.of(session));

        ResponseEntity<ApiResponse<Object>> response = service.updateProfileDetails(
                request("employee", "new@example.com", "0761234567"), Locale.ENGLISH);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(1028, response.getBody().getErrorCode());
        assertFalse(session.isConsumed());
        assertEquals("old@example.com", user.getPrimaryEmail());
        assertEquals("0711234567", user.getPrimaryMobile());
        verify(applicationUserRepository, never()).saveAndFlush(any());
        verify(applicationOtpSessionRepository, never()).saveAndFlush(any());
    }

    @Test
    void latestValidatedOtpSupportsEmailOnlyChange() {
        ApplicationUser user = activeUser();
        ApplicationOtpSession session = validatedSession(user.getId());
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(
                "employee", Status.ACTIVE)).thenReturn(Optional.of(user));
        when(applicationOtpSessionRepository.findTopByApplicationUserIdAndPurposeOrderByCreatedDateDesc(
                user.getId(), "PROFILE_UPDATE")).thenReturn(Optional.of(session));

        ResponseEntity<ApiResponse<Object>> response = service.updateProfileDetails(
                request("employee", "new@example.com", user.getPrimaryMobile()), Locale.ENGLISH);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("new@example.com", user.getPrimaryEmail());
        assertEquals("new@example.com", user.getUserPersonalDetails().getEmail());
        assertTrue(session.isConsumed());
    }

    private ApplicationUser activeUser() {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setEmail("old@example.com");
        personalDetails.setMobileNo("0711234567");

        ApplicationUser user = new ApplicationUser();
        user.setId(10L);
        user.setUsername("employee");
        user.setPrimaryEmail("old@example.com");
        user.setPrimaryMobile("0711234567");
        user.setUserPersonalDetails(personalDetails);
        return user;
    }

    private ApplicationOtpSession validatedSession(Long userId) {
        ApplicationOtpSession session = new ApplicationOtpSession();
        session.setId(55L);
        session.setApplicationUserId(userId);
        session.setPurpose("PROFILE_UPDATE");
        session.setOtp("123456");
        session.setSuccess(true);
        session.setValidated(true);
        session.setConsumed(false);
        session.setCreatedDate(new Date());
        return session;
    }

    private ProfileEditRequestDTO request(String username, String email, String mobile) {
        ProfileEditRequestDTO request = new ProfileEditRequestDTO();
        request.setUsername(username);
        request.setPrimaryEmail(email);
        request.setPrimaryMobile(mobile);
        request.setOtp("123456");
        return request;
    }
}
