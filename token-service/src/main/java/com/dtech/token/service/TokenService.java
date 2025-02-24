package com.dtech.token.service;

import com.dtech.token.dto.request.ChannelRequestDTO;
import com.dtech.token.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface TokenService {
    ResponseEntity<ApiResponse<Object>> getToken(ChannelRequestDTO channelRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> validateToken(String token, Locale locale);
}
