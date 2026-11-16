package com.dtech.claim.service;

import com.dtech.claim.dto.request.DocumentDownloadRequestDTO;
import com.dtech.claim.dto.response.ApiResponse;
import com.dtech.claim.feign.DocumentFeignClient;
import com.dtech.claim.model.Document;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimAttachmentContentServiceTest {
    private final DocumentFeignClient client = mock(DocumentFeignClient.class);
    private final ClaimAttachmentContentService service = new ClaimAttachmentContentService(client);

    @Test
    void readsLegacyDatabaseContentWithoutRemoteCall() {
        Document document = new Document();
        document.setId(1L);
        document.setDoc("/9j/example");

        assertEquals("/9j/example", service.getBase64(document));
        verify(client, never()).getImage(any());
    }

    @Test
    void fetchesObjectStorageContentByDocumentId() {
        Document document = new Document();
        document.setId(601L);
        when(client.getImage(any())).thenAnswer(call -> {
            DocumentDownloadRequestDTO request = call.getArgument(0);
            assertEquals(601L, request.getId());
            return ResponseEntity.ok(ApiResponse.<Object>builder().success(true)
                    .data(Map.of("doc", "/9j/object-content")).build());
        });

        assertEquals("/9j/object-content", service.getBase64(document));
    }

    @Test
    void rejectsEmptyContentInsteadOfReturningSuccessfulNullDocument() {
        Document document = new Document();
        document.setId(602L);
        when(client.getImage(any())).thenReturn(ResponseEntity.ok(
                ApiResponse.<Object>builder().success(true).data(Map.of("fileName", "bill.jpg")).build()));

        assertThrows(IllegalStateException.class, () -> service.getBase64(document));
    }

    @Test
    void rejectsUnavailableDocumentService() {
        Document document = new Document();
        document.setId(603L);
        when(client.getImage(any())).thenReturn(ResponseEntity.status(503).build());

        assertThrows(IllegalStateException.class, () -> service.getBase64(document));
    }
}
