package com.dtech.login.dto.request;

import lombok.Data;

@Data
public class BiometricLoginRequestDTO {

    private String channel;
    private ChannelMbDeviceDetailsDTO deviceDetails;
    private String message;
    private String ip;
    private String username;
    private String uniqueCode;
    private String appVersion;
}
