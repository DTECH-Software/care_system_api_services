package com.dtech.token.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareService")
public class AuditorAwareServiceImpl implements AuditorAware<String> {

    @Autowired
    private HttpServletRequest request;

    @Override
    public Optional<String> getCurrentAuditor() {
        String username = request.getHeader("username");
        return Optional.ofNullable(username);
    }

}