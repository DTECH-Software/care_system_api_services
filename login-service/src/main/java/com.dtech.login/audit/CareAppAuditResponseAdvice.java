package com.dtech.login.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@ControllerAdvice
@RequiredArgsConstructor
public class CareAppAuditResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest serverRequest, ServerHttpResponse serverResponse) {
        Boolean success = extractSuccess(body);
        if (success != null && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            request.setAttribute(CareAppAuditWriter.AUDIT_RESULT_ATTRIBUTE, success ? "SUCCESS" : "FAILED");
        }
        return body;
    }

    private Boolean extractSuccess(Object body) {
        if (body == null) return null;
        try {
            Method getter = body.getClass().getMethod("isSuccess");
            Object value = getter.invoke(body);
            return value instanceof Boolean result ? result : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

