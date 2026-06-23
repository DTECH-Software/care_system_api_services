package com.dtech.claim.controller;

import com.dtech.claim.dto.request.AssistedEmployeeSelectRequestDTO;
import com.dtech.claim.dto.request.validator.AssistedEmployeeSelectValidatorDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.model.ApplicationUser;
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
        return response;
    }
}
