/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 12:35 PM
 * <p>
 */

package com.dtech.message.service.impl;


import com.dtech.message.dto.api.ITextMessageRequestDTO;
import com.dtech.message.dto.request.MessageRequestDTO;
import com.dtech.message.dto.response.ApiResponse;
import com.dtech.message.dto.response.MessageResponseDTO;
import com.dtech.message.enums.NotificationsType;
import com.dtech.message.repository.NotificationTemplateRepository;
import com.dtech.message.service.SendMessageService;
import com.dtech.message.util.ResponseMessageUtil;
import com.dtech.message.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.Locale;


@Service
@Log4j2
@RequiredArgsConstructor
public class SendMessageServiceImpl implements SendMessageService {

    @Autowired
    private final NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private final ResponseUtil responseUtil;

    @Autowired
    private final RestTemplate restTemplate;

    @Value("${message.uri}")
    private String messageURI;

    @Value("${message.api.key}")
    private String apiKey;

    @Autowired
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> sendMessage(MessageRequestDTO messageRequestDTO, Locale locale) {

        try {
            log.info("Processing send message {}", messageRequestDTO);
            MessageResponseDTO sendCustomer = sendToCustomer(messageRequestDTO);
            if(sendCustomer.isSuccess()){
                return ResponseEntity.ok().body(responseUtil.success(null, sendCustomer.getMessage()));
            }

            return ResponseEntity.ok().body(
                    responseUtil.error(null, 1038,
                            sendCustomer.getMessage()));

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional
    public MessageResponseDTO sendToCustomer(MessageRequestDTO messageRequestDTO) {
        try {
            log.info("Processing send message {}", messageRequestDTO);
            return notificationTemplateRepository
                    .findByType(NotificationsType.valueOf(messageRequestDTO.getType())).map((template) -> {

                        ITextMessageRequestDTO iTextMessageRequestDTO = new ITextMessageRequestDTO();
                        iTextMessageRequestDTO.setTo(messageRequestDTO.getMobileNo());
                        String otpMessage = MessageFormat.format(template.getMessageBody(), messageRequestDTO.getValue());
                        iTextMessageRequestDTO.setText(otpMessage);
                        HttpHeaders headers = new HttpHeaders();
                        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
                        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + apiKey);
                        headers.set("X-API-VERSION", "v1");
                        HttpEntity<ITextMessageRequestDTO> entity = new HttpEntity<>(iTextMessageRequestDTO, headers);
                        log.info("Before send message {}", messageRequestDTO);
                        ResponseEntity<String> response = restTemplate.exchange(messageURI, HttpMethod.POST, entity, String.class);
                        log.info("After send message {}", response.toString());
                        MessageResponseDTO responseState = getResponseState(response);
                        responseState.setMessage(messageSource.getMessage("val.otp.send.success", null, null));
                        return responseState;
                    }).orElseGet(() -> {
                        log.info("Template {} not found", messageRequestDTO.getType());
                       return MessageResponseDTO.builder()
                                .success(true)
                                .message(messageSource.getMessage("val.notification.template.not.found", null, null)).build();
                    });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    protected MessageResponseDTO getResponseState(ResponseEntity<String> response) {
        try {
            log.info("Response state for send otp: {}", response.getBody());
            HttpStatusCode statusCode = response.getStatusCode();

            boolean b = switch (statusCode) {
                case HttpStatus.OK -> true;
                default -> false;
            };

            MessageResponseDTO messageResponseDTO = new MessageResponseDTO();
            messageResponseDTO.setSuccess(b);
            return messageResponseDTO;

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }
}


