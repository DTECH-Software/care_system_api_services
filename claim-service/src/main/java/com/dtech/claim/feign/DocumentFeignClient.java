package com.dtech.claim.feign;

import com.dtech.claim.config.FeignConfig;
import com.dtech.claim.dto.request.DocumentDownloadRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


@FeignClient(name = "document-service", fallback = DocumentFeignClientFallback.class,configuration = FeignConfig.class)
public interface DocumentFeignClient {

    @PostMapping("/document-service/api/v1/document/find")
    ResponseEntity<ApiResponse<Object>> getImage(@RequestBody DocumentDownloadRequestDTO documentDownloadRequestDTO);

    @PostMapping(path="/document-service/api/v1/document/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<Object>> upload(@RequestParam("type")String type, @RequestPart("file") MultipartFile file);

}

@Component
class DocumentFeignClientFallback implements DocumentFeignClient {
    @Override
    public ResponseEntity<ApiResponse<Object>> getImage(DocumentDownloadRequestDTO documentDownloadRequestDTO) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }

    @Override
    public ResponseEntity<ApiResponse<Object>> upload(String type, MultipartFile file) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>());
    }

//    @Override
//    public ResponseEntity<byte[]> getImage(DocumentDownloadRequestDTO documentDownloadRequestDTO) {
//        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new byte[0]);
//    }
}
