/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 9:36 AM
 * <p>
 */

package com.dtech.auth.dto.response;


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
