/**
 * User: Himal_J
 * Date: 4/1/2025
 * Time: 8:51 AM
 * <p>
 */

package com.dtech.notification.dto.request;

import lombok.Data;

import java.util.Date;

@Data
public class NotificationHistory {
    private Date fromDate;
    private Date toDate;
    private Boolean read;
}
