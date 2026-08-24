package com.dtech.auth.service.impl;

import com.dtech.auth.dto.request.PolicyDocumentRequestDTO;
import com.dtech.auth.enums.Status;
import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.DocumentStore;
import com.dtech.auth.model.StaffCategories;
import com.dtech.auth.model.UserCompanyDetails;
import com.dtech.auth.model.UserPersonalDetails;
import com.dtech.auth.repository.ApplicationUserRepository;
import com.dtech.auth.repository.DocumentStoreRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServicePolicyDocumentTest {

    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @Mock
    private DocumentStoreRepository documentStoreRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @TempDir
    Path tempDirectory;

    @Test
    void mergesMultipleMedicalPolicyDocumentsForCurrentStaffCategory() throws Exception {
        ApplicationUser applicationUser = applicationUser("employee", "EX-OP2");
        PolicyDocumentRequestDTO request = new PolicyDocumentRequestDTO();
        request.setUsername("employee");
        request.setPolicy(true);

        DocumentStore first = documentStore(
                "POLICY_INS_EX-OP2_DOC_01", createPdf("01-main-policy.pdf", 1));
        DocumentStore second = documentStore(
                "POLICY_INS_EX-OP2_DOC_02", createPdf("02-benefits.pdf", 2));

        when(applicationUserRepository.findByUsernameAndUserPersonalDetails_UserStatus(
                "employee", Status.ACTIVE)).thenReturn(Optional.of(applicationUser));
        when(documentStoreRepository.findAllByCodeStartingWithOrderByCodeAsc(
                "POLICY_INS_EX-OP2_DOC_")).thenReturn(List.of(first, second));

        ResponseEntity<Resource> response = profileService.policyDocument(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"medical-policy-EX-OP2.pdf\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertNotNull(response.getBody());
        try (PDDocument merged = PDDocument.load(response.getBody().getInputStream())) {
            assertEquals(3, merged.getNumberOfPages());
        }
    }

    private ApplicationUser applicationUser(String username, String staffCategoryCode) {
        StaffCategories staffCategory = new StaffCategories();
        staffCategory.setCode(staffCategoryCode);

        UserCompanyDetails companyDetails = new UserCompanyDetails();
        companyDetails.setStaffCategories(staffCategory);

        UserPersonalDetails personalDetails = new UserPersonalDetails();
        personalDetails.setUserCompanyDetails(companyDetails);

        ApplicationUser applicationUser = new ApplicationUser();
        applicationUser.setUsername(username);
        applicationUser.setUserPersonalDetails(personalDetails);
        return applicationUser;
    }

    private DocumentStore documentStore(String code, File file) {
        DocumentStore documentStore = new DocumentStore();
        documentStore.setCode(code);
        documentStore.setPath(file.getAbsolutePath());
        return documentStore;
    }

    private File createPdf(String fileName, int pageCount) throws Exception {
        File file = tempDirectory.resolve(fileName).toFile();
        try (PDDocument document = new PDDocument()) {
            for (int page = 0; page < pageCount; page++) {
                document.addPage(new PDPage());
            }
            document.save(file);
        }
        return file;
    }
}
