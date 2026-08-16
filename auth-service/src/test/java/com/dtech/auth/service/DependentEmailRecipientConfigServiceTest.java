package com.dtech.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DependentEmailRecipientConfigServiceTest {
    private JdbcTemplate jdbcTemplate;
    private DependentEmailRecipientConfigService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new DependentEmailRecipientConfigService(jdbcTemplate);
    }

    @Test
    void activeSubmittedEventUsesConfiguredHrRoleAndCompanyScope() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("DEPENDENT_SUBMITTED")))
                .thenReturn(List.of("ACTIVE"));
        when(jdbcTemplate.queryForList(contains("recipient_type"), eq("DEPENDENT_SUBMITTED")))
                .thenReturn(List.of(Map.of(
                        "recipient_type", "USER_ROLE",
                        "recipient_code", "HRADMIN",
                        "company_scope", "SAME_COMPANY")));
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of("hr@sgcs.lk"));

        assertEquals(List.of("hr@sgcs.lk"), service.resolveSubmittedRecipients("SGCS"));
    }

    @Test
    void missingConfigurationUsesExistingHrAdminQuery() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("DEPENDENT_SUBMITTED")))
                .thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of("legacy@sgcs.lk"));

        assertEquals(List.of("legacy@sgcs.lk"), service.resolveSubmittedRecipients("SGCS"));
    }

    @Test
    void inactiveEventReturnsNoEmailRecipients() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("DEPENDENT_SUBMITTED")))
                .thenReturn(List.of("INACTIVE"));

        assertTrue(service.resolveSubmittedRecipients("SGCS").isEmpty());
        verify(jdbcTemplate, never()).query(anyString(), any(Object[].class), any(RowMapper.class));
    }
}
