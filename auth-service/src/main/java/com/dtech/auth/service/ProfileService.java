package com.dtech.auth.service;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.ApplicationUserDetailsResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface ProfileService {
    ResponseEntity<ApiResponse<Object>> profile(ChannelRequestDTO channelRequestDTO, Locale locale);
}
