package com.dtech.claim.service;

import com.dtech.claim.dto.request.DocumentDownloadRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.dto.response.DocumentDownloadResponseDTO;
import com.dtech.claim.feign.DocumentFeignClient;
import com.dtech.claim.model.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaimAttachmentContentService {
    private final DocumentFeignClient documentFeignClient;

    public String getBase64(Document document) {
        if (StringUtils.hasText(document.getDoc())) {
            return document.getDoc();
        }
        if (document.getId() == null) {
            throw new IllegalStateException("Claim attachment has no document ID");
        }

        ResponseEntity<ApiResponse<Object>> response = documentFeignClient.getImage(
                new DocumentDownloadRequestDTO(document.getId(), false));
        if (response == null || !response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null || !response.getBody().isSuccess()) {
            throw new IllegalStateException("Unable to retrieve claim attachment id=" + document.getId());
        }

        Object data = response.getBody().getData();
        Object content = data instanceof Map<?, ?> map ? map.get("doc")
                : data instanceof DocumentDownloadResponseDTO dto ? dto.getDoc() : null;
        if (!(content instanceof String base64) || !StringUtils.hasText(base64)) {
            throw new IllegalStateException("Claim attachment content is empty id=" + document.getId());
        }
        return base64;
    }
}
