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
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private final DocumentRepository documentRepository;

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
            log.info("upload document from document service {}", documentUploadRequestDTO);
            Document document = gson.fromJson(gson.toJson(documentUploadRequestDTO), Document.class);
            String baseConvert = Base64.getEncoder().encodeToString(documentUploadRequestDTO.getDocument());
            document.setDoc(baseConvert);
            document = documentRepository.saveAndFlush(document);
            log.info("upload document from document service success ");
            return ResponseEntity.ok().body(
                    responseUtil.success(gson.fromJson(gson.toJson(document), DocumentUploadResponseDTO.class),
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
                byte[] bytes = ImageUtils.decodeFromBase64(document.getDoc());
                DocumentDownloadResponseDTO downloadResponseDTO = ImageDownloadMapper.imageDownloadMapper(document);
                downloadResponseDTO.setDocument(bytes);
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
}
