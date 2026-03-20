package com.dtech.auth.dto.request;

import lombok.Data;

@Data
public class BiometricEnableRequestDTO {

    private String channel;
    private ChannelMbDeviceDetailsDTO deviceDetails;
    private String message;
    private Boolean enable;
    private String ip;
    private String username;
}
