/**
 * User: Himal_J
 * Date: 3/8/2025
 * Time: 7:51 PM
 * <p>
 */

package com.dtech.login.feign;

import com.dtech.login.config.FeignConfig;
import com.dtech.login.dto.request.ChannelRequestDTO;
import com.dtech.login.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", fallback = AuthFeignClientFallback.class,configuration = FeignConfig.class)
public interface AuthFeignClient {

    @PostMapping("/auth-service/api/v1/profile/")
    ResponseEntity<ApiResponse<Object>> getProfileDetails(@RequestBody ChannelRequestDTO channelRequestDTO);

}
@Component
class AuthFeignClientFallback implements AuthFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> getProfileDetails(ChannelRequestDTO channelRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }
}

