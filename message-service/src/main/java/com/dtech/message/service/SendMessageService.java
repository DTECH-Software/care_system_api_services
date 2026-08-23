package com.dtech.message.service;

import com.dtech.message.dto.request.MessageRequestDTO;
import com.dtech.message.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface SendMessageService {
    ResponseEntity<ApiResponse<Object>> sendMessage(MessageRequestDTO messageRequestDTO, Locale locale);
}

