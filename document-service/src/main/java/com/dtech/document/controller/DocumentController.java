/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 2:21 PM
 * <p>
 */

package com.dtech.document.controller;

import com.dtech.document.dto.response.DocumentDownloadResponseDTO;
import com.google.gson.Gson;
import com.dtech.document.dto.request.DocumentDownloadRequestDTO;
import com.dtech.document.dto.request.DocumentUploadRequestDTO;
import com.dtech.document.dto.request.validator.DocumentDownloadRequestValidatorDTO;
import com.dtech.document.dto.response.ApiResponse;
import com.dtech.document.service.DocumentService;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;


@RestController
@RequestMapping(path = "api/v1/document")
@Log4j2
@RequiredArgsConstructor
public class DocumentController {

    @Autowired
    private final DocumentService documentService;

    @Autowired
    private final Gson gson;

    @PostMapping(path = "/upload",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Handle document upload request ",notes = "Document upload request success or failed")
    public ResponseEntity<ApiResponse<Object>> upload(@RequestParam("type") String type,
                                                      @RequestParam("file") MultipartFile file, Locale locale) throws IOException {
        log.info("Document upload request controller {} ",type );
        try {
            DocumentUploadRequestDTO validatorDTO = new DocumentUploadRequestDTO();
            validatorDTO.setType(type);
            validatorDTO.setDocument(file.getBytes());
            validatorDTO.setFileName(file.getOriginalFilename());
            validatorDTO.setFileType(file.getContentType());
            log.info("Document upload request after map success {} ",validatorDTO);
            return documentService.upload(validatorDTO,locale);
        }catch (Exception e) {
            log.error(e);
            throw  e;
        }
    }

    @PostMapping(path = "/download",produces = MediaType.MULTIPART_FORM_DATA_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle document download request ",notes = "Document download request success or failed")
    public ResponseEntity<byte[]> download(@RequestBody @Valid DocumentDownloadRequestValidatorDTO documentDownloadRequestValidatorDTO, Locale locale) {
        log.info("Document download request controller {} ", documentDownloadRequestValidatorDTO);
        DocumentDownloadResponseDTO downloadResponseDTO = documentService.download(gson.fromJson(gson.toJson(documentDownloadRequestValidatorDTO), DocumentDownloadRequestDTO.class), locale);

        if(downloadResponseDTO !=null) {
            log.info("Document download request after download success {} ",downloadResponseDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(downloadResponseDTO.getFileType()));
            headers.setContentDispositionFormData("attachment", downloadResponseDTO.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(downloadResponseDTO.getDoc().getBytes());
        }

        return ResponseEntity.ok().build();
   }

    @PostMapping(path = "/find",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Handle document find request ",notes = "Handle document find success or failed")
    public ResponseEntity<ApiResponse<Object>> getDocument(@RequestBody @Valid DocumentDownloadRequestValidatorDTO documentDownloadRequestValidatorDTO, Locale locale) {
        log.info("Document find request controller {} ", documentDownloadRequestValidatorDTO);
        return documentService.getDocument(gson.fromJson(gson.toJson(documentDownloadRequestValidatorDTO), DocumentDownloadRequestDTO.class), locale);
    }
}
