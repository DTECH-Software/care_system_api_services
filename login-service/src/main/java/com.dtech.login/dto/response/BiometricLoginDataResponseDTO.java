package com.dtech.login.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BiometricLoginDataResponseDTO {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("tokenExpiresIn")
    private Long tokenExpiresIn;

    @JsonProperty("viewAllOptions")
    private boolean viewAllOptions;

    @JsonProperty("lastLoggedDate")
    private String lastLoggedDate;
}
