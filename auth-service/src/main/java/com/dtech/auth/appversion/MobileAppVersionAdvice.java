package com.dtech.auth.appversion;

import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@ControllerAdvice
@RequiredArgsConstructor
@Log4j2
public class MobileAppVersionAdvice implements RequestBodyAdvice, ResponseBodyAdvice<Object> {
    public static final String AUDIT_CLIENT_VERSION_ATTRIBUTE = "care.audit.client-app-version";
    public static final String AUDIT_CLIENT_PLATFORM_ATTRIBUTE = "care.audit.client-platform";
    public static final String AUDIT_UPDATE_STATUS_ATTRIBUTE = "care.audit.app-update-status";
    private static final String RESPONSE_ATTRIBUTE = "care.app-version.response";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+(?:\\.\\d+){0,3}(?:[-+][0-9A-Za-z.-]+)?$");
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;
    private final ResponseUtil responseUtil;
    private final Map<String, CachedConfig> cache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (!"MB".equalsIgnoreCase(readString(body, "getChannel"))) return body;

        String clientVersion = clean(readString(body, "getAppVersion"));
        String platform = resolvePlatform(body);
        MobileAppVersionResponse versionResponse = evaluate(platform, clientVersion);

        currentRequest().ifPresent(request -> {
            request.setAttribute(RESPONSE_ATTRIBUTE, versionResponse);
            request.setAttribute(AUDIT_CLIENT_VERSION_ATTRIBUTE, clientVersion);
            request.setAttribute(AUDIT_CLIENT_PLATFORM_ATTRIBUTE, platform);
            request.setAttribute(AUDIT_UPDATE_STATUS_ATTRIBUTE, versionResponse.getUpdateStatus());
        });

        if ("REQUIRED".equals(versionResponse.getUpdateStatus())) {
            throw new MobileAppVersionException(requiredMessage(clientVersion, versionResponse));
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        currentResponse().ifPresent(version -> {
            if (body instanceof ApiResponse<?> apiResponse) apiResponse.setAppVersion(version);
            addHeader(response, "X-Client-App-Version", version.getClientVersion());
            addHeader(response, "X-Latest-App-Version", version.getLatestVersion());
            addHeader(response, "X-Minimum-Supported-App-Version", version.getMinimumSupportedVersion());
            addHeader(response, "X-App-Update-Status", version.getUpdateStatus());
        });
        return body;
    }

    @ExceptionHandler(MobileAppVersionException.class)
    public ResponseEntity<ApiResponse<Object>> handleVersionException(MobileAppVersionException exception) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(responseUtil.error(null, 1060, exception.getMessage()));
    }

    private MobileAppVersionResponse evaluate(String platform, String clientVersion) {
        if (clientVersion != null && !VERSION_PATTERN.matcher(clientVersion).matches()) {
            return response(platform, clientVersion, null, "REQUIRED");
        }

        VersionConfig config = loadConfig(platform);
        if (config == null) return response(platform, clientVersion, null, "UNKNOWN");

        String updateStatus;
        if (clientVersion == null) {
            updateStatus = config.forceUpdate() ? "REQUIRED" : "UNKNOWN";
        } else if (compareVersions(clientVersion, config.minimumSupportedVersion()) < 0) {
            updateStatus = "REQUIRED";
        } else if (compareVersions(clientVersion, config.latestVersion()) < 0) {
            updateStatus = config.forceUpdate() ? "REQUIRED" : "OPTIONAL";
        } else {
            updateStatus = "NONE";
        }
        return response(platform, clientVersion, config, updateStatus);
    }

    private MobileAppVersionResponse response(String platform, String clientVersion,
                                              VersionConfig config, String updateStatus) {
        return MobileAppVersionResponse.builder()
                .clientVersion(clientVersion)
                .latestVersion(config == null ? null : config.latestVersion())
                .minimumSupportedVersion(config == null ? null : config.minimumSupportedVersion())
                .updateStatus(updateStatus)
                .platform(platform)
                .storeUrl(config == null ? null : config.storeUrl())
                .releaseNotes(config == null ? null : config.releaseNotes())
                .build();
    }

    private VersionConfig loadConfig(String platform) {
        if (platform == null) return null;
        CachedConfig cached = cache.get(platform);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return cached.config();
        }
        try {
            List<VersionConfig> rows = jdbcTemplate.query("""
                    SELECT latest_version, minimum_supported_version, force_update,
                           store_url, release_notes
                    FROM mobile_app_version_config
                    WHERE platform = ? AND status = 'ACTIVE'
                    LIMIT 1
                    """, (resultSet, rowNum) -> new VersionConfig(
                    resultSet.getString("latest_version"),
                    resultSet.getString("minimum_supported_version"),
                    resultSet.getBoolean("force_update"),
                    resultSet.getString("store_url"),
                    resultSet.getString("release_notes")), platform);
            VersionConfig config = rows.isEmpty() ? null : rows.get(0);
            cache.put(platform, new CachedConfig(config, Instant.now()));
            return config;
        } catch (Exception exception) {
            log.warn("Unable to load mobile app version configuration for platform={}; request will not be blocked",
                    platform, exception);
            return null;
        }
    }

    static int compareVersions(String left, String right) {
        int[] leftParts = numericParts(left);
        int[] rightParts = numericParts(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftValue = index < leftParts.length ? leftParts[index] : 0;
            int rightValue = index < rightParts.length ? rightParts[index] : 0;
            if (leftValue != rightValue) return Integer.compare(leftValue, rightValue);
        }
        return 0;
    }

    private static int[] numericParts(String version) {
        String core = version.split("[-+]", 2)[0];
        String[] values = core.split("\\.");
        int[] parts = new int[values.length];
        for (int index = 0; index < values.length; index++) parts[index] = Integer.parseInt(values[index]);
        return parts;
    }

    private String resolvePlatform(Object body) {
        String explicitPlatform = normalizePlatform(readString(body, "getPlatform"));
        if (explicitPlatform != null) return explicitPlatform;
        Object deviceDetails = invoke(body, "getDeviceDetails");
        return normalizePlatform(readString(deviceDetails, "getDeviceOS"));
    }

    private String normalizePlatform(String value) {
        String cleaned = clean(value);
        if (cleaned == null) return null;
        String normalized = cleaned.toUpperCase(Locale.ROOT);
        if (normalized.contains("ANDROID")) return "ANDROID";
        if (normalized.contains("IOS") || normalized.contains("IPHONE") || normalized.contains("IPAD")) return "IOS";
        return null;
    }

    private String requiredMessage(String clientVersion, MobileAppVersionResponse response) {
        if (clientVersion == null) return "appVersion is required for mobile requests";
        if (!VERSION_PATTERN.matcher(clientVersion).matches()) return "Invalid appVersion format";
        return "Please update the mobile application to continue";
    }

    private String readString(Object target, String getter) {
        Object value = invoke(target, getter);
        return value == null ? null : value.toString();
    }

    private Object invoke(Object target, String getter) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(getter);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private java.util.Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return java.util.Optional.of(attributes.getRequest());
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<MobileAppVersionResponse> currentResponse() {
        return currentRequest().map(request -> request.getAttribute(RESPONSE_ATTRIBUTE))
                .filter(MobileAppVersionResponse.class::isInstance)
                .map(MobileAppVersionResponse.class::cast);
    }

    private void addHeader(ServerHttpResponse response, String name, String value) {
        if (value != null && !value.isBlank()) response.getHeaders().set(name, value);
    }

    private record VersionConfig(String latestVersion, String minimumSupportedVersion,
                                 boolean forceUpdate, String storeUrl, String releaseNotes) {
    }

    private record CachedConfig(VersionConfig config, Instant loadedAt) {
    }

    private static final class MobileAppVersionException extends RuntimeException {
        private MobileAppVersionException(String message) {
            super(message);
        }
    }
}
