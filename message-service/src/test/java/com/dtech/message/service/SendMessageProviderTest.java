package com.dtech.message.service;

import com.dtech.message.dto.request.MessageRequestDTO;
import com.dtech.message.dto.response.MessageResponseDTO;
import com.dtech.message.enums.MessageType;
import com.dtech.message.model.NotificationTemplate;
import com.dtech.message.repository.NotificationTemplateRepository;
import com.dtech.message.service.impl.SendMessageServiceImpl;
import com.dtech.message.util.ResponseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class SendMessageProviderTest {
    @Test
    void routesExistingOtpTemplateToHutch() {
        NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
        NotificationTemplate template = new NotificationTemplate();
        template.setType(MessageType.OTP);
        template.setMessageBody("Your OTP is {0}");
        when(repository.findByType(MessageType.OTP)).thenReturn(Optional.of(template));

        HutchSmsClient hutch = mock(HutchSmsClient.class);
        when(hutch.send(eq("0712345678"), eq("Your OTP is 123456")))
                .thenReturn(MessageResponseDTO.builder().success(true).build());
        SendMessageServiceImpl service = new SendMessageServiceImpl(
                repository, mock(ResponseUtil.class), mock(RestTemplate.class), hutch, mock(MessageSource.class));
        ReflectionTestUtils.setField(service, "provider", "hutch");

        assertTrue(service.sendToCustomer(new MessageRequestDTO("0712345678", "OTP", "123456")).isSuccess());
        verify(hutch).send("0712345678", "Your OTP is 123456");
    }

    @Test
    void propagatesHutchFailureToExistingOtpCaller() {
        NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
        NotificationTemplate template = new NotificationTemplate();
        template.setType(MessageType.OTP);
        template.setMessageBody("Your OTP is {0}");
        when(repository.findByType(MessageType.OTP)).thenReturn(Optional.of(template));

        HutchSmsClient hutch = mock(HutchSmsClient.class);
        when(hutch.send("0712345678", "Your OTP is 123456"))
                .thenReturn(MessageResponseDTO.builder().success(false).message("SMS provider request failed").build());
        SendMessageServiceImpl service = new SendMessageServiceImpl(
                repository, mock(ResponseUtil.class), mock(RestTemplate.class), hutch, mock(MessageSource.class));
        ReflectionTestUtils.setField(service, "provider", "hutch");

        assertFalse(service.sendToCustomer(new MessageRequestDTO("0712345678", "OTP", "123456")).isSuccess());
    }

    @Test
    void sendsPlainTextClaimTemplateViaHutch() {
        NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
        NotificationTemplate template = new NotificationTemplate();
        template.setMessageBody("Claim {0} submitted.<br><br> Thank you!");
        when(repository.findByType(MessageType.INSURANCE_CLAIM)).thenReturn(Optional.of(template));
        HutchSmsClient hutch = mock(HutchSmsClient.class);
        when(hutch.send("0712345678", "Claim 123 submitted.\n\nThank you!"))
                .thenReturn(MessageResponseDTO.builder().success(true).build());
        SendMessageServiceImpl service = new SendMessageServiceImpl(
                repository, mock(ResponseUtil.class), mock(RestTemplate.class), hutch, mock(MessageSource.class));
        ReflectionTestUtils.setField(service, "provider", "hutch");

        assertTrue(service.sendToCustomer(new MessageRequestDTO("0712345678", "INSURANCE_CLAIM", "123"))
                .isSuccess());
        verify(hutch).send("0712345678", "Claim 123 submitted.\n\nThank you!");
    }

    @Test
    void sendsPlainTextClaimTemplateViaTextIt() {
        NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
        NotificationTemplate template = new NotificationTemplate();
        template.setMessageBody("Claim {0} submitted.<br><br> Thank you!");
        when(repository.findByType(MessageType.INSURANCE_CLAIM)).thenReturn(Optional.of(template));
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1}"));
        SendMessageServiceImpl service = new SendMessageServiceImpl(
                repository, mock(ResponseUtil.class), restTemplate, mock(HutchSmsClient.class), mock(MessageSource.class));
        ReflectionTestUtils.setField(service, "provider", "textit");
        ReflectionTestUtils.setField(service, "messageURI", "https://api.textit.biz/");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        service.sendToCustomer(new MessageRequestDTO("0712345678", "INSURANCE_CLAIM", "123"));

        ArgumentCaptor<HttpEntity> sent = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), sent.capture(), eq(String.class));
        com.dtech.message.dto.api.ITextMessageRequestDTO body =
                (com.dtech.message.dto.api.ITextMessageRequestDTO) sent.getValue().getBody();
        assertTrue(body.getText().contains("submitted.\n\nThank you!"));
        assertFalse(body.getText().contains("<br"));
    }
}
