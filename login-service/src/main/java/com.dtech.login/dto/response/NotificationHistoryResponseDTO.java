package com.dtech.login.dto.response;

import lombok.Data;

@Data
public class NotificationHistoryResponseDTO {
    private Long id;
    private String title;
    private String titleDescription;
    private String body;
    private boolean isRead;
    private long agoDays;
}