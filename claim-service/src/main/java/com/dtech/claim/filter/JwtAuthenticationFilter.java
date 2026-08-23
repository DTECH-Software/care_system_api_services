/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:24 PM
 * <p>
 */

package com.dtech.claim.filter;

import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.TokenValidResponseDTO;
import com.dtech.claim.feign.TokenFeignClient;
import com.dtech.claim.util.ExtractApiResponseUtil;
import com.dtech.claim.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Log4j2
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenFeignClient tokenFeignClient;
    private final Gson gson;
    private final ResponseUtil responseUtil;

    public JwtAuthenticationFilter(TokenFeignClient tokenFeignClient, Gson gson, ResponseUtil responseUtil) {
        this.tokenFeignClient = tokenFeignClient;
        this.gson = gson;
        this.responseUtil = responseUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JWT Authentication Filter Started {}, {}", request.getRequestURI(), request);
        try {
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                log.info("JWT Authentication Filter Authorized - Auth");
                String token = authorization.substring(7);
                log.info("JWT Authentication Filter Token - Auth {}", token);
                log.info("JWT Authentication Filter request to token server - Auth {}", token);

                ResponseEntity<ApiResponse<Object>> validateTokenResponse = tokenFeignClient.validateToken(token);
                log.info("After response token service Auth {}", validateTokenResponse);
                Object objectApiResponse = ExtractApiResponseUtil.extractApiResponse(validateTokenResponse);
                log.info("After response token response Auth {}", objectApiResponse);

                TokenValidResponseDTO tokenValidResponseDTO = gson.fromJson(gson.toJson(objectApiResponse), TokenValidResponseDTO.class);

                if (!token.isBlank() && tokenValidResponseDTO.isValid()) {
                    log.info("JWT Authentication Filter Token valid - Auth {}", tokenValidResponseDTO);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            tokenValidResponseDTO.getUsername(), null, new ArrayList<>()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                }else {
                    log.info("JWT Authentication Filter Token expired or invalid - Auth {}", token);
                    sendUnauthorizedResponse(response, "Token has expired or is invalid. Please log in again to continue");
                    return ;
                }
            }
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error(e);
            sendUnauthorizedResponse(null,"An error occurred while processing the token");
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<Object> apiResponse = responseUtil.error(
                null,
                1111,
                message
        );
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
    }

}
