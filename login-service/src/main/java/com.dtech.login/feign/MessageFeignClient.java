package com.dtech.login.feign;

import com.dtech.login.dto.request.MessageRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service", fallback = MessageFeignClientFallback.class)
public interface MessageFeignClient {

    @PostMapping("/message-service/api/v1/text/send")
    ResponseEntity<ApiResponse<Object>> sendMessage(@RequestBody MessageRequestDTO messageRequestDTO);

}

@Component
class MessageFeignClientFallback implements MessageFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> sendMessage(MessageRequestDTO messageRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }
}
