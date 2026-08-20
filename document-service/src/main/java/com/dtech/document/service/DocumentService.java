package com.dtech.document.service;

import com.dtech.document.dto.request.DocumentDownloadRequestDTO;
import com.dtech.document.dto.request.DocumentUploadRequestDTO;
import com.dtech.document.dto.response.ApiResponse;
import com.dtech.document.dto.response.DocumentBinaryDownloadResponseDTO;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Locale;

public interface DocumentService {
    ResponseEntity<ApiResponse<Object>> upload(DocumentUploadRequestDTO documentUploadRequestDTO, Locale locale) throws IOException;
    DocumentBinaryDownloadResponseDTO download(DocumentDownloadRequestDTO documentDownloadRequestDTO, Locale locale);
    ResponseEntity<ApiResponse<Object>> getDocument(DocumentDownloadRequestDTO documentDownloadRequestDTO, Locale locale);
}
