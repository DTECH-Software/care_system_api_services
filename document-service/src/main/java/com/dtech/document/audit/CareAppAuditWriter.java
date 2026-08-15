package com.dtech.document.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class CareAppAuditWriter {
    public static final String AUDIT_USERNAME_ATTRIBUTE = "care.audit.username";
    public static final String AUDIT_RESULT_ATTRIBUTE = "care.audit.result";
    public static final String AUDIT_CORRELATION_ATTRIBUTE = "care.audit.correlation-id";
    private static final int MAX_SHORT_TEXT = 255;

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:document-service}")
    private String module;

    public void write(HttpServletRequest request, int responseStatus, long durationMs, Throwable failure) {
        try {
            String requestPath = limit(request.getRequestURI(), MAX_SHORT_TEXT);
            String method = limit(request.getMethod(), 10);
            String action = limit(method + " " + requestPath, 100);
            String result = resolveResult(request, responseStatus, failure);
            String username = resolveUsername(request);
            String correlationId = resolveCorrelationId(request);
            String taskDescription = limit(result + " - " + action, MAX_SHORT_TEXT);

            jdbcTemplate.update("""
                    INSERT INTO audit_log
                    (old_value, new_value, ip_address, user_agent, task, task_description, page,
                     source, module, action, result, response_status, request_path, http_method,
                     duration_ms, correlation_id, created_date, last_modified_date,
                     created_user, last_modified_user)
                    VALUES (NULL, NULL, ?, ?, 'API_REQUEST', ?, NULL,
                            'WECARE_APP', ?, ?, ?, ?, ?, ?, ?, ?,
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                    """,
                    resolveIp(request), limit(request.getHeader("User-Agent"), MAX_SHORT_TEXT),
                    taskDescription, module, action, result, responseStatus, requestPath, method,
                    durationMs, correlationId, username, username);
        } catch (Exception e) {
            // Audit failure must never interrupt the user's business request.
            log.error("Unable to save Care-App audit metadata for module={} path={}",
                    module, request.getRequestURI(), e);
        }
    }

    private String resolveUsername(HttpServletRequest request) {
        Object username = request.getAttribute(AUDIT_USERNAME_ATTRIBUTE);
        if (username != null && !username.toString().isBlank()) return limit(username.toString(), 100);
        return "anonymous";
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object generated = request.getAttribute(AUDIT_CORRELATION_ATTRIBUTE);
        if (generated != null) return limit(generated.toString(), 64);
        String value = request.getHeader("X-Correlation-ID");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : limit(value, 64);
    }

    private String resolveResult(HttpServletRequest request, int responseStatus, Throwable failure) {
        Object businessResult = request.getAttribute(AUDIT_RESULT_ATTRIBUTE);
        if (failure != null || responseStatus >= 400) return "FAILED";
        return businessResult == null ? "SUCCESS" : limit(businessResult.toString().toUpperCase(), 20);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return limit(ip == null ? "unknown" : ip, 45);
    }

    private String limit(String value, int max) {
        if (value == null || value.isBlank()) return "unknown";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
