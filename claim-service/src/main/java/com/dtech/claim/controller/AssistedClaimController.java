package com.dtech.claim.controller;

import com.dtech.claim.dto.request.AssistedEmployeeSelectRequestDTO;
import com.dtech.claim.dto.request.validator.AssistedEmployeeSelectValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.model.ApplicationUser;
import com.dtech.claim.model.Document;
import com.dtech.claim.model.UserAddress;
import com.dtech.claim.model.UserCompanyDetails;
import com.dtech.claim.model.UserPersonalDetails;
import com.dtech.claim.service.AssistedUserResolver;
import com.dtech.claim.util.ResponseMessageUtil;
import com.dtech.claim.util.ResponseUtil;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(path = "api/v1/assisted")
@Log4j2
@RequiredArgsConstructor
public class AssistedClaimController {
    private final AssistedUserResolver assistedUserResolver;
    private final ResponseUtil responseUtil;
    private final MessageSource messageSource;
    private final Gson gson;

    @PostMapping(path = "/employee-select", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> selectEmployee(@RequestBody @Valid AssistedEmployeeSelectValidatorDTO validatorDTO,
                                                              Locale locale) {
        AssistedEmployeeSelectRequestDTO request = gson.fromJson(gson.toJson(validatorDTO), AssistedEmployeeSelectRequestDTO.class);
        AssistedUserResolver.SelectionResult result = assistedUserResolver.selectEmployee(request.getUsername(), request.getEpfNo(), request.getCompany());
        if (!result.found()) {
            return ResponseEntity.ok().body(responseUtil.error(null, 1014,
                    messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale)));
        }
        if (result.selectionRequired()) {
            return ResponseEntity.ok().body(responseUtil.success(buildSelectionRequiredResponse(request.getEpfNo(), result.employees()),
                    "Multiple employees found. Please select company."));
        }
        return ResponseEntity.ok().body(responseUtil.success(buildResponse(result.applicationUser()), "Assisted employee selected successfully"));
    }

    private Object buildSelectionRequiredResponse(String epfNo, java.util.List<Map<String, Object>> employees) {
        Map<String, Object> response = new HashMap<>();
        response.put("selectionRequired", true);
        response.put("epfNo", epfNo);
        response.put("employees", employees);
        return response;
    }

    private Object buildResponse(ApplicationUser user) {
        Map<String, Object> response = new HashMap<>();
        response.put("assistedMode", true);
        response.put("actingEmployeeId", user.getId());
        response.put("username", user.getUsername());
        response.put("primaryEmail", user.getPrimaryEmail());
        boolean hasRealMobile = assistedUserResolver.hasRealMobile(user.getPrimaryMobile());
        response.put("primaryMobile", mobileForResponse(user.getPrimaryMobile(), hasRealMobile));
        response.put("hasRealMobile", hasRealMobile);
        response.put("lastPasswordChangeDate", formatDateTime(user.getLastPasswordChangeDate()));
        response.put("lastLoggedDate", formatDateTime(user.getLastLoggedDate()));
        response.put("expectingFirstTimeLogging", user.isExpectingFirstTimeLogging());
        response.put("expectingDependentsRegister", user.isExpectingDependentsRegister());
        response.put("passwordExpiredDate", formatDate(user.getPasswordExpiredDate()));
        response.put("epfNo", user.getUserPersonalDetails().getEpfNo());
        response.put("employeeName", (user.getUserPersonalDetails().getFirstName() + " " + user.getUserPersonalDetails().getLastName()).trim());
        response.put("company", user.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
        response.put("companyDescription", user.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getDescription());
        response.put("staffCategory", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
        response.put("staffCategoryDescription", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getDescription());
        response.put("userPersonalDetails", buildUserPersonalDetails(user));
        response.put("profileImg", buildDocument(user.getProfileImg()));
        response.put("notification", Map.of("unreadCount", 0, "latestNotifications", java.util.List.of()));
        response.put("createdDate", formatDate(user.getCreatedDate()));
        response.put("facilityId", user.getFacilityId());
        response.put("userType", null);
        response.put("assistedClaim", false);
        response.put("roleCode", null);
        response.put("roleDescription", null);
        response.put("reset", user.isReset());
        return response;
    }

    static String mobileForResponse(String mobile, boolean hasRealMobile) {
        return hasRealMobile ? mobile : null;
    }

    private Map<String, Object> buildUserPersonalDetails(ApplicationUser user) {
        UserPersonalDetails personalDetails = user.getUserPersonalDetails();
        Map<String, Object> details = new HashMap<>();
        details.put("id", personalDetails.getId());
        details.put("epfNo", personalDetails.getEpfNo());
        details.put("initials", personalDetails.getInitials());
        details.put("firstName", personalDetails.getFirstName());
        details.put("lastName", personalDetails.getLastName());
        details.put("nic", personalDetails.getNic());
        details.put("email", personalDetails.getEmail());
        details.put("mobileNo", assistedUserResolver.hasRealMobile(personalDetails.getMobileNo()) ? personalDetails.getMobileNo() : null);
        details.put("gender", personalDetails.getGender() != null ? personalDetails.getGender().name() : null);
        details.put("genderDescription", personalDetails.getGender() != null ? personalDetails.getGender().getDescription() : null);
        details.put("title", personalDetails.getTitle() != null ? personalDetails.getTitle().name() : null);
        details.put("titleDescription", personalDetails.getTitle() != null ? personalDetails.getTitle().getDescription() : null);
        details.put("maritalStatus", personalDetails.getMaritalStatus() != null ? personalDetails.getMaritalStatus().name() : null);
        details.put("maritalStatusDescription", personalDetails.getMaritalStatus() != null ? personalDetails.getMaritalStatus().getDescription() : null);
        details.put("dob", personalDetails.getDob());
        details.put("age", resolveAge(personalDetails.getDob()));
        details.put("userStatus", personalDetails.getUserStatus() != null ? personalDetails.getUserStatus().name() : null);
        details.put("isTemp", personalDetails.getIsTemp());
        details.put("tempId", personalDetails.getTempId());
        details.put("birthImg", buildDocument(personalDetails.getBirthImg()));
        details.put("userAddress", buildAddress(personalDetails.getUserAddress()));
        details.put("userCompanyDetails", buildCompanyDetails(personalDetails.getUserCompanyDetails()));
        return details;
    }

    private Map<String, Object> buildAddress(UserAddress address) {
        if (address == null) {
            return null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("streetNo", address.getStreetNo());
        response.put("street1", address.getStreet1());
        response.put("street2", address.getStreet2());
        response.put("city", address.getCity());
        return response;
    }

    private Map<String, Object> buildCompanyDetails(UserCompanyDetails companyDetails) {
        if (companyDetails == null) {
            return null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("companyTypes", base(companyDetails.getCompanyTypes() != null ? companyDetails.getCompanyTypes().getCode() : null,
                companyDetails.getCompanyTypes() != null ? companyDetails.getCompanyTypes().getDescription() : null));
        response.put("staffCategories", base(companyDetails.getStaffCategories() != null ? companyDetails.getStaffCategories().getCode() : null,
                companyDetails.getStaffCategories() != null ? companyDetails.getStaffCategories().getDescription() : null));
        response.put("staffTypes", base(companyDetails.getStaffTypes() != null ? companyDetails.getStaffTypes().getCode() : null,
                companyDetails.getStaffTypes() != null ? companyDetails.getStaffTypes().getDescription() : null));
        response.put("designation", companyDetails.getDesignation());
        if (companyDetails.getPreviousPermanentDate() == null) {
            response.put("permanentDate", null);
            response.put("previousPermanentDate", formatDate(companyDetails.getPermanentDate()));
        } else {
            response.put("permanentDate", formatDate(companyDetails.getPermanentDate()));
            response.put("previousPermanentDate", formatDate(companyDetails.getPreviousPermanentDate()));
        }
        response.put("transferDate", formatDate(companyDetails.getTransferDate()));
        response.put("terminateDate", formatDate(companyDetails.getTerminateDate()));
        response.put("insurancePolicy", base(companyDetails.getInsurancePolicy() != null ? companyDetails.getInsurancePolicy().getCode() : null,
                companyDetails.getInsurancePolicy() != null ? companyDetails.getInsurancePolicy().getDescription() : null));
        response.put("facility", companyDetails.getFacility() != null ? companyDetails.getFacility().name() : null);
        response.put("facilityDescription", companyDetails.getFacility() != null ? companyDetails.getFacility().name() : null);
        return response;
    }

    private Map<String, Object> base(String code, String description) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("description", description);
        return response;
    }

    private String formatDate(java.util.Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String formatDateTime(java.util.Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private Map<String, Object> buildDocument(Document document) {
        if (document == null) {
            return null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("type", document.getType() != null ? document.getType().name() : null);
        response.put("fileName", document.getFileName());
        response.put("fileType", document.getFileType());
        response.put("doc", document.getDoc());
        return response;
    }

    private Integer resolveAge(java.util.Date dob) {
        if (dob == null) {
            return null;
        }
        java.time.LocalDate birthDate = java.time.Instant.ofEpochMilli(dob.getTime())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }
}
