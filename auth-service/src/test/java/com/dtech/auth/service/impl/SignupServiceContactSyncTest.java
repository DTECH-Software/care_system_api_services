package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.UserPersonalDetailsRequestDTO;
import com.dtech.auth.feign.MessageFeignClient;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.OnboardingRequest;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.*;
import com.dtech.auth.util.FacilityIdGenUtil;
import com.dtech.auth.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupServiceContactSyncTest {

    private UserPersonalDetailsRepository userPersonalDetailsRepository;
    private ApplicationUserRepository applicationUserRepository;
    private SignupServiceImpl service;

    @BeforeEach
    void setUp() {
        userPersonalDetailsRepository = mock(UserPersonalDetailsRepository.class);
        applicationUserRepository = mock(ApplicationUserRepository.class);
        FacilityIdGenUtil facilityIdGenUtil = mock(FacilityIdGenUtil.class);
        EntityManager entityManager = mock(EntityManager.class);
        SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);

        when(entityManager.unwrap(SharedSessionContractImplementor.class)).thenReturn(session);
        when(facilityIdGenUtil.generate(session, null)).thenReturn("0001");
        when(userPersonalDetailsRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationUserRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new SignupServiceImpl(
                userPersonalDetailsRepository,
                applicationUserRepository,
                mock(MessageSource.class),
                new ResponseUtil(),
                new ModelMapper(),
                mock(OnboardingVerifiedMobileRepository.class),
                mock(ApplicationPasswordPolicyRepository.class),
                mock(ApplicationOtpSessionRepository.class),
                new Gson(),
                mock(MessageFeignClient.class),
                mock(OnboardingRequestRepository.class),
                mock(ApplicationPasswordHistoryRepository.class),
                mock(ApplicationUsernamePolicyRepository.class),
                mock(CompanyTypesRepository.class),
                mock(StaffCategoriesRepository.class),
                mock(StaffTypesRepository.class),
                new ObjectMapper(),
                mock(MarriedRepository.class),
                mock(TreatmentRepository.class),
                mock(TreatmentCategoryRepository.class),
                facilityIdGenUtil,
                entityManager,
                mock(RejoinProcessingService.class));
    }

    @Test
    void onboardingWritesSameNormalizedContactsToBothTables() {
        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setEmail("old@example.com");
        personalDetails.setMobileNo("0711111111");

        UserPersonalDetailsRequestDTO request = new UserPersonalDetailsRequestDTO();
        request.setUsername("employee");
        request.setEmail(" New.Email@Example.COM ");
        request.setMobileNo(" 0761234567 ");

        ApplicationUser applicationUser = service.updateApplicationUser(
                request, "hashed-password", "salt", new OnboardingRequest(), personalDetails);

        assertEquals("new.email@example.com", applicationUser.getPrimaryEmail());
        assertEquals("0761234567", applicationUser.getPrimaryMobile());
        assertEquals("new.email@example.com", personalDetails.getEmail());
        assertEquals("0761234567", personalDetails.getMobileNo());
        assertSame(personalDetails, applicationUser.getUserPersonalDetails());
        verify(userPersonalDetailsRepository).saveAndFlush(personalDetails);
        verify(applicationUserRepository).saveAndFlush(applicationUser);
    }
}
