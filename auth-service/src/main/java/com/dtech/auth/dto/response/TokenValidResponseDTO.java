package com.dtech.auth.dto.response;

import lombok.Data;

@Data
public class TokenValidResponseDTO {
    private boolean valid;
    private String username;
}