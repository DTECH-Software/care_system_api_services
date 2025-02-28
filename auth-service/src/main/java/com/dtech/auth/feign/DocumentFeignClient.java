package com.dtech.auth.feign;

import com.dtech.auth.config.FeignConfig;
import com.dtech.auth.dto.request.DocumentDownloadRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "document-service", fallback = DocumentFeignClientFallback.class,configuration = FeignConfig.class)
public interface DocumentFeignClient {

    @PostMapping("/document-service/api/v1/document/download")
    ResponseEntity<byte[]> getImage(@RequestBody DocumentDownloadRequestDTO documentDownloadRequestDTO);

}

@Component
class DocumentFeignClientFallback implements DocumentFeignClient {

    @Override
    public ResponseEntity<byte[]> getImage(DocumentDownloadRequestDTO documentDownloadRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new byte[0]);
    }
}

