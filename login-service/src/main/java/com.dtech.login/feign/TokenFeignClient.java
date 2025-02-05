package com.dtech.login.feign;

import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "token-service", fallback = TokenFeignClientFallback.class)
public interface TokenFeignClient {

    @PostMapping("/token-service/api/v1/token/issuer-token")
    ResponseEntity<ApiResponse<Object>> getToken(@RequestBody ChannelRequestDTO channelRequestDTO);

}

@Component
class TokenFeignClientFallback implements TokenFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> getToken(ChannelRequestDTO channelRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }
}
