package com.dtech.claim.controller;

import com.dtech.claim.dto.request.AssistedEmployeeSelectRequestDTO;
import com.dtech.claim.dto.request.validator.AssistedEmployeeSelectValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.model.ApplicationUser;
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
        return assistedUserResolver.selectEmployee(request.getUsername(), request.getEpfNo())
                .map(user -> ResponseEntity.ok().body(responseUtil.success(buildResponse(user), "Assisted employee selected successfully")))
                .orElseGet(() -> ResponseEntity.ok().body(responseUtil.error(null, 1014,
                        messageSource.getMessage(ResponseMessageUtil.APPLICATION_USER_NOT_FOUND, null, locale))));
    }

    private Object buildResponse(ApplicationUser user) {
        Map<String, Object> response = new HashMap<>();
        response.put("assistedMode", true);
        response.put("actingEmployeeId", user.getId());
        response.put("username", user.getUsername());
        response.put("epfNo", user.getUserPersonalDetails().getEpfNo());
        response.put("employeeName", (user.getUserPersonalDetails().getFirstName() + " " + user.getUserPersonalDetails().getLastName()).trim());
        response.put("company", user.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getCode());
        response.put("companyDescription", user.getUserPersonalDetails().getUserCompanyDetails().getCompanyTypes().getDescription());
        response.put("staffCategory", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getCode());
        response.put("staffCategoryDescription", user.getUserPersonalDetails().getUserCompanyDetails().getStaffCategories().getDescription());
        response.put("userPersonalDetails", buildUserPersonalDetails(user));
        return response;
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
        details.put("mobileNo", personalDetails.getMobileNo());
        details.put("maritalStatus", personalDetails.getMaritalStatus() != null ? personalDetails.getMaritalStatus().name() : null);
        details.put("maritalStatusDescription", personalDetails.getMaritalStatus() != null ? personalDetails.getMaritalStatus().getDescription() : null);
        details.put("dob", formatDate(personalDetails.getDob()));
        details.put("userStatus", personalDetails.getUserStatus() != null ? personalDetails.getUserStatus().name() : null);
        details.put("isTemp", personalDetails.getIsTemp());
        details.put("tempId", personalDetails.getTempId());
        details.put("createdDate", formatDate(user.getCreatedDate()));
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
        response.put("permanentDate", formatDate(companyDetails.getPermanentDate()));
        response.put("previousPermanentDate", formatDate(companyDetails.getPreviousPermanentDate()));
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
}
