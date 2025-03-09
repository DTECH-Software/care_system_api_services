/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 1:35 PM
 * <p>
 */

package com.dtech.document.dto.response;

import lombok.Data;

@Data
public class DocumentUploadResponseDTO {
    private Long id;
    private String type;
    private String doc;
    private String fileName;
    private String fileType;
}
