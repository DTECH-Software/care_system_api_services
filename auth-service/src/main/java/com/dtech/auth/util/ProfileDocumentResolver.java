package com.dtech.auth.util;

import com.dtech.auth.dto.request.DocumentDownloadRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.DocumentDownloadResponseDTO;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.model.Document;
import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProfileDocumentResolver {

    public static DocumentDownloadResponseDTO resolve(
            Document document,
            DocumentFeignClient documentFeignClient,
            Gson gson) {
        if (document == null) {
            return null;
        }

        DocumentDownloadResponseDTO fallback = new DocumentDownloadResponseDTO(
                document.getType() == null ? null : document.getType().name(),
                document.getFileName(),
                document.getFileType(),
                document.getDoc());

        if (document.getId() == null) {
            return fallback;
        }

        try {
            ResponseEntity<ApiResponse<Object>> response = documentFeignClient.getImage(
                    new DocumentDownloadRequestDTO(document.getId(), false));
            Object responseData = ExtractApiResponseUtil.extractApiResponse(response);
            if (responseData == null) {
                log.warn("Document service returned no data for profile document id={}", document.getId());
                return fallback;
            }

            DocumentDownloadResponseDTO resolved = gson.fromJson(
                    gson.toJson(responseData), DocumentDownloadResponseDTO.class);
            if (resolved == null || resolved.getDoc() == null || resolved.getDoc().isBlank()) {
                log.warn("Document service returned no content for profile document id={}", document.getId());
                return fallback;
            }
            return resolved;
        } catch (Exception exception) {
            log.warn("Unable to load profile document id={} from document service", document.getId(), exception);
            return fallback;
        }
    }
}
