package com.dtech.notification.appversion;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileAppVersionResponse {
    private String clientVersion;
    private String latestVersion;
    private String minimumSupportedVersion;
    private String updateStatus;
    private String platform;
    private String storeUrl;
    private String releaseNotes;
}

