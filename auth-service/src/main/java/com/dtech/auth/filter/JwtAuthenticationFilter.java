/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 2:24 PM
 * <p>
 */

package com.dtech.auth.filter;

import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.TokenValidResponseDTO;
import com.dtech.auth.feign.TokenFeignClient;
import com.dtech.auth.util.ExtractApiResponseUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

@Log4j2
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenFeignClient tokenFeignClient;
    private final Gson gson;

    public JwtAuthenticationFilter(TokenFeignClient tokenFeignClient, Gson gson) {
        this.tokenFeignClient = tokenFeignClient;
        this.gson = gson;
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
                String username = extractUsernameFromRequestBody(request);
                ResponseEntity<ApiResponse<Object>> validateTokenResponse = tokenFeignClient.validateToken(token, username);
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

                }
            }
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private String extractUsernameFromRequestBody(HttpServletRequest request) throws IOException {

        try {
            log.info("JWT Authentication Filter Request extract username from body- Auth {}", request);
            String username = "";
            StringBuilder body = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            JsonObject jsonObject = JsonParser.parseString(body.toString()).getAsJsonObject();
            log.info("JWT Authentication Filter Request extract username from body json reader result - Auth {}",jsonObject);
            if (jsonObject.has("username")) {
                username = jsonObject.get("username").getAsString();
            }
            return username;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
