package com.dtech.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
public class DocumentBinaryDownloadResponseDTO {
    @ToString.Exclude
    private byte[] content;
    private String fileName;
    private String fileType;
}
