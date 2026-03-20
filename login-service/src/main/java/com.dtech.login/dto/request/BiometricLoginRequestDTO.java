package com.dtech.login.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BiometricLoginRequestDTO {

    @JsonProperty("dtechUserId")
    private String dtechUserId;

    @JsonProperty("uniqueCode")
    private String uniqueCode;

    @JsonProperty("deviceChannel")
    private String deviceChannel;

    @JsonProperty("messageVersion")
    private String messageVersion;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("deviceInfo")
    private String deviceInfo;

    @JsonProperty("dtechTransId")
    private String dtechTransId;

    @JsonProperty("appID")
    private String appId;

    @JsonProperty("appMaxTimeout")
    private String appMaxTimeout;

    @JsonProperty("appReferenceNumber")
    private String appReferenceNumber;

    @JsonProperty("appTransID")
    private String appTransId;
}
