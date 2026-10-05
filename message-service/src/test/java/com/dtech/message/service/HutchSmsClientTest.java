package com.dtech.message.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HutchSmsClientTest {
    private static final String BASE_URL = "https://bsms.hutch.lk";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizesSriLankanMobileNumbers() {
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("0712345678"));
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("712345678"));
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("+94712345678"));
        assertEquals("94712345678", HutchSmsClient.normalizeMobile("071 234 5678"));
        assertEquals(null, HutchSmsClient.normalizeMobile("12345"));
    }

    @Test
    void sendsWithApprovedMaskAndReusesAccessToken() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(eq(BASE_URL + "/api/login"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("{\"accessToken\":\"token\"}")));
        when(restTemplate.exchange(eq(BASE_URL + "/api/sendsms"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<?> entity = invocation.getArgument(2);
                    assertEquals("Bearer token", entity.getHeaders().getFirst("Authorization"));
                    assertEquals("v1", entity.getHeaders().getFirst("X-API-VERSION"));
                    assertEquals("Info WeCare", ((java.util.Map<?, ?>) entity.getBody()).get("mask"));
                    assertEquals("94712345678", ((java.util.Map<?, ?>) entity.getBody()).get("numbers"));
                    return ResponseEntity.ok(json("{\"serverRef\":32225}"));
                });

        HutchSmsClient client = client(restTemplate);
        assertTrue(client.send("0712345678", "Test OTP").isSuccess());
        assertTrue(client.send("0712345678", "Test OTP").isSuccess());
        verify(restTemplate, times(1)).exchange(eq(BASE_URL + "/api/login"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void renewsTokenAndRetriesOnceOnUnauthorized() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(eq(BASE_URL + "/api/login"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("{\"accessToken\":\"first\"}")))
                .thenReturn(ResponseEntity.ok(json("{\"accessToken\":\"second\"}")));
        when(restTemplate.exchange(eq(BASE_URL + "/api/sendsms"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, null, null))
                .thenReturn(ResponseEntity.ok(json("{\"serverRef\":32226}")));

        assertTrue(client(restTemplate).send("0712345678", "Test OTP").isSuccess());
        verify(restTemplate, times(2)).exchange(eq(BASE_URL + "/api/login"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class));
        verify(restTemplate, times(2)).exchange(eq(BASE_URL + "/api/sendsms"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    void rejectsMissingConfigurationWithoutNetworkCall() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HutchSmsClient client = new HutchSmsClient(restTemplate, BASE_URL, "", "", "", "WeCare");
        assertFalse(client.send("0712345678", "Test OTP").isSuccess());
    }

    private HutchSmsClient client(RestTemplate restTemplate) {
        return new HutchSmsClient(restTemplate, BASE_URL, "test-user", "test-password", "Info WeCare", "WeCare");
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
