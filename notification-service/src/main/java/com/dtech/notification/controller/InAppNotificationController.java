/**
 * User: Himal_J
 * Date: 3/4/2025
 * Time: 9:44 PM
 * <p>
 */

package com.dtech.notification.controller;

import com.dtech.notification.dto.request.NotificationHistory;
import com.dtech.notification.dto.request.PaginationRequest;
import com.dtech.notification.dto.response.ApiResponse;
import com.dtech.notification.service.InAppNotificationService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.Locale;

@RestController
@RequestMapping(path = "api/v1/in-app")
@Log4j2
@RequiredArgsConstructor
public class InAppNotificationController {

    @Autowired
    private final InAppNotificationService inAppNotificationService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/filter-list",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle in-app notification request request ",notes = "In-app notification request success or failed")
    public ResponseEntity<ApiResponse<Object>> inAppNotificationHistory(@RequestBody @Valid PaginationRequest<NotificationHistory> paginationRequest, Locale locale) {
        log.info("In-app notification request controller {} ", paginationRequest);
        Type paginationRequestType = new TypeToken<PaginationRequest<NotificationHistory>>(){}.getType();
        return inAppNotificationService.inAppNotificationHistory(gson.fromJson(gson.toJson(paginationRequest), paginationRequestType), locale);
    }

}
