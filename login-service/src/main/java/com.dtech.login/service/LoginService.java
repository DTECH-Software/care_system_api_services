package com.dtech.login.service;

import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.request.LoginRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface LoginService {
    ResponseEntity<ApiResponse<Object>> logIn(LoginRequestDTO loginRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> logOut(ChannelRequestDTO channelRequestDTO, Locale locale);
}
