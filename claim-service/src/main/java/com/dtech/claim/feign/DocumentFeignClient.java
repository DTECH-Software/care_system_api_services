package com.dtech.claim.feign;

import com.dtech.claim.config.FeignConfig;
import com.dtech.claim.dto.request.DocumentDownloadRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "document-service", fallback = DocumentFeignClientFallback.class,configuration = FeignConfig.class)
public interface DocumentFeignClient {

//    @PostMapping("/document-service/api/v1/document/download")
//    ResponseEntity<byte[]> getImage(@RequestBody DocumentDownloadRequestDTO documentDownloadRequestDTO);
//
    @PostMapping("/document-service/api/v1/document/find")
    ResponseEntity<ApiResponse<Object>> getImage(@RequestBody DocumentDownloadRequestDTO documentDownloadRequestDTO);


}

@Component
class DocumentFeignClientFallback implements DocumentFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> getImage(DocumentDownloadRequestDTO documentDownloadRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }

//    @Override
//    public ResponseEntity<byte[]> getImage(DocumentDownloadRequestDTO documentDownloadRequestDTO) {
//        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new byte[0]);
//    }
}

