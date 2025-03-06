package com.dtech.claim.dto.response;

import lombok.Data;

@Data
public class TokenValidResponseDTO {
    private boolean valid;
    private String username;
}