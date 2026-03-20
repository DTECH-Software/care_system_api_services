package com.dtech.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BiometricEnableResponseDTO {

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("messageVersion")
    private String messageVersion;

    @JsonProperty("deviceChannel")
    private String deviceChannel;

    @JsonProperty("dtechTransId")
    private String dtechTransId;

    @JsonProperty("appTransID")
    private String appTransId;

    @JsonProperty("responseCode")
    private String responseCode;

    @JsonProperty("responseDescription")
    private String responseDescription;

    @JsonProperty("dtechUserID")
    private String dtechUserId;

    @JsonProperty("data")
    private BiometricEnableDataResponseDTO data;
}
