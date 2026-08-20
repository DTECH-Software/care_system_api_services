package com.dtech.document.service;

import com.dtech.document.dto.request.DocumentDownloadRequestDTO;
import com.dtech.document.dto.response.DocumentBinaryDownloadResponseDTO;
import com.dtech.document.model.Document;
import com.dtech.document.repository.DocumentRepository;
import com.dtech.document.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentServiceImplTest {

    @Test
    void returnsOriginalPdfBytesForBinaryDownload() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentStorageService documentStorageService = mock(DocumentStorageService.class);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentRepository,
                documentStorageService,
                null,
                null,
                null
        );
        byte[] pdfBytes = "%PDF-1.7\nwecare-test".getBytes(StandardCharsets.US_ASCII);
        Document document = new Document();
        document.setId(2519L);
        document.setFileName("birth-certificate.pdf");
        document.setFileType("application/pdf");
        when(documentRepository.findById(2519L)).thenReturn(Optional.of(document));
        when(documentStorageService.getBase64(document))
                .thenReturn(Base64.getEncoder().encodeToString(pdfBytes));

        DocumentDownloadRequestDTO request = new DocumentDownloadRequestDTO();
        request.setId(2519L);
        DocumentBinaryDownloadResponseDTO response = service.download(request, Locale.ENGLISH);

        assertArrayEquals(pdfBytes, response.getContent());
        assertEquals("application/pdf", response.getFileType());
        assertEquals("birth-certificate.pdf", response.getFileName());
    }
}
