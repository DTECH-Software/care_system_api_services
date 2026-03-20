package com.dtech.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BiometricEnableDataResponseDTO {

    @JsonProperty("uniqueCode")
    private String uniqueCode;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("mobileNo")
    private String mobileNo;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nic")
    private String nic;

    @JsonProperty("profileImageKey")
    private String profileImageKey;
}
