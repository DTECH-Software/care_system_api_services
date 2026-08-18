package com.dtech.auth.service;

import com.dtech.auth.dto.request.MaritalStatusRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.enums.RequestType;
import com.dtech.auth.enums.Status;
import com.dtech.auth.enums.Workflow;
import com.dtech.auth.feign.DocumentFeignClient;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceMaritalStatusTest {

    private ApplicationUserRepository applicationUserRepository;
    private MaritalStatusRepository maritalStatusRepository;
    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        applicationUserRepository = mock(ApplicationUserRepository.class);
        maritalStatusRepository = mock(MaritalStatusRepository.class);
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
                mock(ApplicationOtpSessionRepository.class),
                maritalStatusRepository);
    }

    @Test
    void divorceRequestStoresDivorceAsRequestedStatus() {
        ApplicationUser user = userWithStatus(com.dtech.auth.enums.MaritalStatus.MARRIED);
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus("employee", Status.ACTIVE))
                .thenReturn(Optional.of(user));

        MaritalStatusRequestDTO request = request(RequestType.DIVORCE);
        ResponseEntity<ApiResponse<Object>> response = service.updateMaritalStatus(request, Locale.ENGLISH);

        assertTrue(response.getBody().isSuccess());
        assertNotNull(user.getMaritalStatus());
        assertEquals(com.dtech.auth.enums.MaritalStatus.DIVORCE,
                user.getMaritalStatus().getMaritalStatus());
        assertEquals(Workflow.UNDER_REVIEW, user.getMaritalStatus().getStatus());
        verify(applicationUserRepository).saveAndFlush(user);
    }

    @Test
    void secondRequestIsRejectedWhilePreviousRequestIsUnderReview() {
        ApplicationUser user = userWithStatus(com.dtech.auth.enums.MaritalStatus.UNMARRIED);
        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus("employee", Status.ACTIVE))
                .thenReturn(Optional.of(user));
        when(maritalStatusRepository.existsByApplicationUserAndStatus(user, Workflow.UNDER_REVIEW))
                .thenReturn(true);

        ResponseEntity<ApiResponse<Object>> response = service.updateMaritalStatus(
                request(RequestType.MARRIED), Locale.ENGLISH);

        assertFalse(response.getBody().isSuccess());
        assertEquals(1045, response.getBody().getErrorCode());
        verify(applicationUserRepository, never()).saveAndFlush(any());
    }

    private ApplicationUser userWithStatus(com.dtech.auth.enums.MaritalStatus status) {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setMaritalStatus(status);
        ApplicationUser user = new ApplicationUser();
        user.setUsername("employee");
        user.setUserPersonalDetails(personalDetails);
        return user;
    }

    private MaritalStatusRequestDTO request(RequestType requestType) {
        MaritalStatusRequestDTO request = new MaritalStatusRequestDTO();
        request.setUsername("employee");
        request.setRequestType(requestType.name());
        request.setDocuments(List.of());
        return request;
    }
}
