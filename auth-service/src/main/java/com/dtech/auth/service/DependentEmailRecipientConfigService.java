package com.dtech.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class DependentEmailRecipientConfigService {
    private static final String EVENT_CODE = "DEPENDENT_SUBMITTED";

    private final JdbcTemplate jdbcTemplate;

    public List<String> resolveSubmittedRecipients(String companyCode) {
        if (companyCode == null || companyCode.isBlank()) return List.of();
        try {
            List<String> eventStatuses = jdbcTemplate.queryForList(
                    "SELECT status FROM email_notification_event WHERE code = ?",
                    String.class,
                    EVENT_CODE);
            if (eventStatuses.isEmpty()) return legacyFallback(companyCode, "event is not configured");
            if (!"ACTIVE".equalsIgnoreCase(eventStatuses.get(0))) return List.of();

            List<Map<String, Object>> rules = jdbcTemplate.queryForList("""
                    SELECT r.recipient_type, r.recipient_code, r.company_scope
                    FROM email_notification_recipient_rule r
                    JOIN email_notification_event e ON e.id = r.event_id
                    WHERE e.code = ? AND e.status = 'ACTIVE' AND r.status = 'ACTIVE'
                    """, EVENT_CODE);
            if (rules.isEmpty()) return List.of();

            Set<String> emails = new LinkedHashSet<>();
            for (Map<String, Object> rule : rules) {
                String recipientType = Objects.toString(rule.get("recipient_type"), "");
                String recipientCode = Objects.toString(rule.get("recipient_code"), "");
                String companyScope = Objects.toString(rule.get("company_scope"), "ALL_COMPANIES");
                resolveRule(recipientType, recipientCode, companyScope, companyCode).stream()
                        .filter(email -> email != null && !email.isBlank())
                        .map(String::trim)
                        .forEach(emails::add);
            }
            return new ArrayList<>(emails);
        } catch (RuntimeException ex) {
            log.error("Unable to load DB recipients for {}. Using existing HRADMIN routing.", EVENT_CODE, ex);
            return legacyFallback(companyCode, "configuration lookup failed");
        }
    }

    private List<String> resolveRule(String recipientType,
                                     String recipientCode,
                                     String companyScope,
                                     String companyCode) {
        if (recipientType.isBlank() || recipientCode.isBlank()) return List.of();

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT wu.email
                FROM web_user wu
                LEFT JOIN web_user_role wur ON wu.user_role = wur.code
                WHERE wu.status = 'ACTIVE'
                  AND wu.email IS NOT NULL
                  AND TRIM(wu.email) <> ''
                  AND (
                       (? = 'USER_ROLE' AND UPPER(wu.user_role) = UPPER(?) AND wur.status = 'ACTIVE')
                    OR (? = 'APPROVAL_LEVEL' AND UPPER(wu.approval_level) = UPPER(?))
                    OR (? = 'SPECIFIC_USER' AND UPPER(wu.username) = UPPER(?))
                  )
                """);
        List<Object> parameters = new ArrayList<>(List.of(
                recipientType, recipientCode,
                recipientType, recipientCode,
                recipientType, recipientCode));

        if ("SAME_COMPANY".equalsIgnoreCase(companyScope)) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM web_user_company wuc
                        JOIN company_types ct ON ct.id = wuc.company_id
                        WHERE wuc.web_user_id = wu.id
                          AND ct.status = 'ACTIVE'
                          AND UPPER(ct.code) = UPPER(?)
                    )
                    """);
            parameters.add(companyCode);
        } else if ("SAME_COMPANY_OR_UNASSIGNED".equalsIgnoreCase(companyScope)) {
            sql.append("""
                    AND (
                        NOT EXISTS (SELECT 1 FROM web_user_company none_assigned WHERE none_assigned.web_user_id = wu.id)
                        OR EXISTS (
                            SELECT 1
                            FROM web_user_company wuc
                            JOIN company_types ct ON ct.id = wuc.company_id
                            WHERE wuc.web_user_id = wu.id
                              AND ct.status = 'ACTIVE'
                              AND UPPER(ct.code) = UPPER(?)
                        )
                    )
                    """);
            parameters.add(companyCode);
        }

        return jdbcTemplate.query(sql.toString(), parameters.toArray(),
                (rs, rowNum) -> rs.getString("email"));
    }

    private List<String> legacyFallback(String companyCode, String reason) {
        log.warn("Using legacy recipient routing for {} because {}", EVENT_CODE, reason);
        return jdbcTemplate.query("""
                        SELECT DISTINCT wu.email
                        FROM web_user wu
                        JOIN web_user_role wur ON wu.user_role = wur.code
                        JOIN web_user_company wuc ON wu.id = wuc.web_user_id
                        JOIN company_types ct ON wuc.company_id = ct.id
                        WHERE wu.status = 'ACTIVE'
                          AND wur.status = 'ACTIVE'
                          AND ct.status = 'ACTIVE'
                          AND ct.code = ?
                          AND UPPER(wur.code) = 'HRADMIN'
                          AND wu.email IS NOT NULL
                          AND TRIM(wu.email) <> ''
                        """,
                new Object[]{companyCode},
                (rs, rowNum) -> rs.getString("email"));
    }
}
