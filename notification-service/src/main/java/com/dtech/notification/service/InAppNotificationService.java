package com.dtech.notification.service;

import com.dtech.notification.dto.request.NotificationHistory;
import com.dtech.notification.dto.request.PaginationRequest;
import com.dtech.notification.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface InAppNotificationService {
    ResponseEntity<ApiResponse<Object>> inAppNotificationHistory(PaginationRequest<NotificationHistory> paginationRequest, Locale locale);
}
