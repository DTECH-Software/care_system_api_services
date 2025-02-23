package com.dtech.auth.feign;

import com.dtech.auth.dto.request.ChannelRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "token-service", fallback = TokenFeignClientFallback.class)
public interface TokenFeignClient {

    @PostMapping("/token-service/api/v1/token/issuer-token")
    ResponseEntity<ApiResponse<Object>> getToken(@RequestBody ChannelRequestDTO channelRequestDTO);

    @PostMapping("/token-service/api/v1/token/validate-token")
    ResponseEntity<ApiResponse<Object>> validateToken(@RequestParam(name = "token") String token,@RequestParam(name = "username")String username);

}

@Component
class TokenFeignClientFallback implements TokenFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> getToken(ChannelRequestDTO channelRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> validateToken(String token,String username) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }
}
