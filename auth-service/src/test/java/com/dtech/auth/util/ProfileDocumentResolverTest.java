package com.dtech.auth.util;

import com.dtech.auth.dto.request.DocumentDownloadRequestDTO;
import com.dtech.auth.dto.response.ApiResponse;
import com.dtech.auth.dto.response.DocumentDownloadResponseDTO;
import com.dtech.auth.enums.DocType;
import com.dtech.auth.feign.DocumentFeignClient;
import com.dtech.auth.model.Document;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileDocumentResolverTest {

    private final DocumentFeignClient documentFeignClient = mock(DocumentFeignClient.class);
    private final Gson gson = new Gson();

    @Test
    void loadsObjectStorageContentForPersistedProfileDocument() {
        Document document = document(2519L, null);
        DocumentDownloadResponseDTO downloaded = new DocumentDownloadResponseDTO(
                "PROFILE", "profile.png", "image/png", "base64-from-object-storage");
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(true)
                .data(downloaded)
                .build();
        when(documentFeignClient.getImage(org.mockito.ArgumentMatchers.any(DocumentDownloadRequestDTO.class)))
                .thenReturn(ResponseEntity.ok(apiResponse));

        DocumentDownloadResponseDTO result = ProfileDocumentResolver.resolve(
                document, documentFeignClient, gson);

        assertEquals("base64-from-object-storage", result.getDoc());
        assertEquals("profile.png", result.getFileName());
        ArgumentCaptor<DocumentDownloadRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(DocumentDownloadRequestDTO.class);
        verify(documentFeignClient).getImage(requestCaptor.capture());
        assertEquals(2519L, requestCaptor.getValue().getId());
        assertEquals(false, requestCaptor.getValue().isState());
    }

    @Test
    void keepsStoredMetadataWhenDocumentServiceIsUnavailable() {
        Document document = document(2519L, null);
        when(documentFeignClient.getImage(org.mockito.ArgumentMatchers.any(DocumentDownloadRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiResponse<>()));

        DocumentDownloadResponseDTO result = ProfileDocumentResolver.resolve(
                document, documentFeignClient, gson);

        assertNull(result.getDoc());
        assertEquals("profile.png", result.getFileName());
        assertEquals("image/png", result.getFileType());
    }

    @Test
    void doesNotCallDocumentServiceForUnsavedDocument() {
        Document document = document(null, "database-base64");

        DocumentDownloadResponseDTO result = ProfileDocumentResolver.resolve(
                document, documentFeignClient, gson);

        assertEquals("database-base64", result.getDoc());
        verifyNoInteractions(documentFeignClient);
    }

    private Document document(Long id, String doc) {
        Document document = new Document();
        document.setId(id);
        document.setType(DocType.PROFILE);
        document.setFileName("profile.png");
        document.setFileType("image/png");
        document.setDoc(doc);
        return document;
    }
}
