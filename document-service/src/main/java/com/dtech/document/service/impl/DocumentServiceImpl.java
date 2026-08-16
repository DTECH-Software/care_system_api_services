/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 12:51 PM
 * <p>
 */

package com.dtech.document.service.impl;

import com.dtech.document.dto.response.DocumentDownloadResponseDTO;
import com.dtech.document.dto.response.DocumentUploadResponseDTO;
import com.dtech.document.mapper.EntityToDto.ImageDownloadMapper;
import com.dtech.document.util.ImageUtils;
import com.google.gson.Gson;
import com.dtech.document.dto.request.DocumentDownloadRequestDTO;
import com.dtech.document.dto.request.DocumentUploadRequestDTO;
import com.dtech.document.dto.response.ApiResponse;
import com.dtech.document.model.Document;
import com.dtech.document.repository.DocumentRepository;
import com.dtech.document.service.DocumentService;
import com.dtech.document.service.DocumentStorageService;
import com.dtech.document.util.ResponseMessageUtil;
import com.dtech.document.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private final DocumentRepository documentRepository;

    @Autowired
    private final DocumentStorageService documentStorageService;

    @Autowired
    private final Gson gson;

    @Autowired
    private final MessageSource messageSource;

    @Autowired
    private final ResponseUtil responseUtil;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Object>> upload(DocumentUploadRequestDTO documentUploadRequestDTO, Locale locale) throws IOException {

        try {
            log.info("Upload document type={} fileName={} fileType={}",
                    documentUploadRequestDTO.getType(), documentUploadRequestDTO.getFileName(),
                    documentUploadRequestDTO.getFileType());
            Document document = gson.fromJson(gson.toJson(documentUploadRequestDTO), Document.class);
            document = documentStorageService.saveAppDocument(document, documentUploadRequestDTO.getDocument());
            DocumentUploadResponseDTO uploadResponse = toUploadResponse(document);
            log.info("upload document from document service success ");
            return ResponseEntity.ok().body(
                    responseUtil.success(uploadResponse,
                            messageSource.getMessage(ResponseMessageUtil.DOCUMENT_UPLOAD_SUCCESS, null, locale))
            );
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownloadResponseDTO download(DocumentDownloadRequestDTO documentDownloadRequestDTO, Locale locale) {

        try {
            log.info("download document from document service {}", documentDownloadRequestDTO);
            Optional<Document> documentOpt = documentRepository.findById(documentDownloadRequestDTO.getId());
            return documentOpt.map(document -> {
                log.info("Download document found from document service");
                byte[] bytes = ImageUtils.decodeFromBase64(documentStorageService.getBase64(document));
                DocumentDownloadResponseDTO downloadResponseDTO = ImageDownloadMapper.imageDownloadMapper(document);
                downloadResponseDTO.setDoc(Arrays.toString(bytes));
                return downloadResponseDTO;
            }).orElseGet(() -> {
                log.error("Document not found with ID: " + documentDownloadRequestDTO.getId());
                return null;
            });

        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getDocument(DocumentDownloadRequestDTO documentDownloadRequestDTO, Locale locale) {
        try {
            log.info("find document from document service {}", documentDownloadRequestDTO);
            return documentRepository.findById(documentDownloadRequestDTO.getId()).map((doc) -> {
                log.info("Document found from document service");
                Object response = null;
                String base64 = documentStorageService.getBase64(doc);
                if (documentDownloadRequestDTO.isState()) {
                    log.info("Document true state found with ID: " + documentDownloadRequestDTO.getId());
                    response = toUploadResponse(doc, base64);
                } else {
                    log.info("Document false state found with ID: " + documentDownloadRequestDTO.getId());
                    DocumentDownloadResponseDTO downloadResponse = ImageDownloadMapper.imageDownloadMapper(doc);
                    downloadResponse.setDoc(base64);
                    response = downloadResponse;
                }
                log.info("download document from document service success");
                return ResponseEntity.ok().body(responseUtil.success(response, messageSource.getMessage(ResponseMessageUtil.DOCUMENT_DOWNLOAD_SUCCESS, null, locale)));
            }).orElseGet(() -> {
                log.error("Document not found with ID: " + documentDownloadRequestDTO.getId());
                return ResponseEntity.ok().body(responseUtil.error(null, 1026, messageSource.getMessage(ResponseMessageUtil.DOCUMENT_NOT_FOUND, null, locale)));
            });
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

    private DocumentUploadResponseDTO toUploadResponse(Document document) {
        return toUploadResponse(document, documentStorageService.getBase64(document));
    }

    private DocumentUploadResponseDTO toUploadResponse(Document document, String base64) {
        DocumentUploadResponseDTO response = new DocumentUploadResponseDTO();
        response.setId(document.getId());
        response.setType(document.getType() == null ? null : document.getType().name());
        response.setDoc(base64);
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        return response;
    }
}
