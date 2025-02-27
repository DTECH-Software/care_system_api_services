package com.dtech.document.service;

import com.dtech.document.dto.request.DocumentDownloadRequestDTO;
import com.dtech.document.dto.request.DocumentUploadRequestDTO;
import com.dtech.document.dto.response.ApiResponse;
import com.dtech.document.dto.response.DocumentDownloadResponseDTO;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Locale;

public interface DocumentService {
    ResponseEntity<ApiResponse<Object>> upload(DocumentUploadRequestDTO documentUploadRequestDTO, Locale locale) throws IOException;
    DocumentDownloadResponseDTO download(DocumentDownloadRequestDTO documentDownloadRequestDTO, Locale locale);
}
