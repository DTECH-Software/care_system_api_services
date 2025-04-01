package com.dtech.login.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class NotificationSummaryResponseDTO {
    private long unreadCount;
    private List<NotificationHistoryResponseDTO> latestNotifications;
}
