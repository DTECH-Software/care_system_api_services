package com.dtech.document.feign;

import com.dtech.document.config.FeignConfig;
import com.dtech.document.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "token-service", fallback = TokenFeignClientFallback.class,configuration = FeignConfig.class)
public interface TokenFeignClient {

    @PostMapping("/token-service/api/v1/token/validate-token")
    ResponseEntity<ApiResponse<Object>> validateToken(@RequestParam(name = "token") String token);

}

@Component
class TokenFeignClientFallback implements TokenFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> validateToken(String token) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }
}
