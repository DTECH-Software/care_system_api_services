/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 9:23 AM
 * <p>
 */

package com.dtech.message.dto.response;

import com.dtech.message.appversion.MobileAppVersionResponse;
import com.dtech.message.maintenance.MaintenanceResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private int errorCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MobileAppVersionResponse appVersion;
    private boolean underMaintenance;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MaintenanceResponse maintenance;
}
