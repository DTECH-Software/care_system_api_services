/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 11:53 AM
 * <p>
 */

package com.dtech.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationSummaryResponseDTO {
    private long unreadCount;
    private List<NotificationHistoryResponseDTO> latestNotifications;
}
