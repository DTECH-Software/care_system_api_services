package com.dtech.auth.service;

import com.dtech.auth.dto.request.ClaimDependentDetailsRequestDTO;
import com.dtech.auth.dto.request.ClaimDependentRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.enums.Gender;
import com.dtech.auth.enums.MaritalStatus;
import com.dtech.auth.enums.RelationCategory;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileServiceParentAgeTest {

    private ClaimDependentsRepository claimDependentsRepository;
    private DocumentFeignClient documentFeignClient;
    private AssistedUserResolver assistedUserResolver;
    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        claimDependentsRepository = mock(ClaimDependentsRepository.class);
        documentFeignClient = mock(DocumentFeignClient.class);
        assistedUserResolver = mock(AssistedUserResolver.class);
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), isNull(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new ProfileServiceImpl(
                mock(ApplicationUserRepository.class),
                messageSource,
                new ResponseUtil(),
                claimDependentsRepository,
                new Gson(),
                assistedUserResolver,
                documentFeignClient,
                mock(MarriedRepository.class),
                mock(NotificationHistoryRepository.class),
                mock(DocumentStoreRepository.class),
                mock(EmailNotificationService.class),
                mock(DependentEmailRecipientConfigService.class),
                mock(ApplicationOtpSessionRepository.class),
                mock(MaritalStatusRepository.class));
    }

    @Test
    void parentWithSameCompletedAgeAsEmployeeIsRejected() {
        ClaimDependentRequestDTO request = parentRequest(dateOfBirth(30, 1));
        when(assistedUserResolver.resolve(request)).thenReturn(Optional.of(user(dateOfBirth(30, 0))));

        ResponseEntity<ApiResponse<Object>> response = service.addDependents(request, Locale.ENGLISH);

        assertParentAgeError(response);
        verifyNoInteractions(claimDependentsRepository, documentFeignClient);
    }

    @Test
    void parentYoungerThanEmployeeIsRejected() {
        ClaimDependentRequestDTO request = parentRequest(dateOfBirth(29, 0));
        when(assistedUserResolver.resolve(request)).thenReturn(Optional.of(user(dateOfBirth(30, 0))));

        ResponseEntity<ApiResponse<Object>> response = service.addDependents(request, Locale.ENGLISH);

        assertParentAgeError(response);
        verifyNoInteractions(claimDependentsRepository, documentFeignClient);
    }

    @Test
    void parentOlderThanEmployeePassesAgeValidation() {
        ApplicationUser user = user(dateOfBirth(30, 0));
        ClaimDependentRequestDTO request = parentRequest(dateOfBirth(31, 0));
        when(assistedUserResolver.resolve(request)).thenReturn(Optional.of(user));
        when(claimDependentsRepository.existsAllByApplicationUserAndRelationCategoryAndStatusIn(
                user,
                RelationCategory.MOTHER,
                List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW)))
                .thenReturn(true);

        ResponseEntity<ApiResponse<Object>> response = service.addDependents(request, Locale.ENGLISH);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(1022, response.getBody().getErrorCode());
        verify(claimDependentsRepository).existsAllByApplicationUserAndRelationCategoryAndStatusIn(
                user,
                RelationCategory.MOTHER,
                List.of(Workflow.APPROVED, Workflow.UNDER_REVIEW));
        verifyNoInteractions(documentFeignClient);
    }

    private void assertParentAgeError(ResponseEntity<ApiResponse<Object>> response) {
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(1047, response.getBody().getErrorCode());
        assertEquals("val.dependent.parent.must.be.older.than.employee", response.getBody().getMessage());
    }

    private ApplicationUser user(Date employeeDob) {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setDob(employeeDob);
        personalDetails.setMaritalStatus(MaritalStatus.UNMARRIED);
        personalDetails.setGender(Gender.MALE);

        ApplicationUser user = new ApplicationUser();
        user.setUsername("employee");
        user.setUserPersonalDetails(personalDetails);
        return user;
    }

    private ClaimDependentRequestDTO parentRequest(Date parentDob) {
        ClaimDependentDetailsRequestDTO parent = new ClaimDependentDetailsRequestDTO();
        parent.setRelationCategory(RelationCategory.MOTHER.name());
        parent.setGender(Gender.FEMALE.name());
        parent.setDob(parentDob);

        ClaimDependentRequestDTO request = new ClaimDependentRequestDTO();
        request.setUsername("employee");
        request.setDependents(List.of(parent));
        return request;
    }

    private Date dateOfBirth(int yearsAgo, int additionalDaysAgo) {
        LocalDate date = LocalDate.now()
                .minusYears(yearsAgo)
                .minusDays(additionalDaysAgo);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
