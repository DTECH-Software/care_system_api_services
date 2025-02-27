/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 2:10 PM
 * <p>
 */

package com.dtech.document.dto.response;


import lombok.Data;

@Data
public class DocumentDownloadResponseDTO {
    private String type;
    private byte[] document;
    private String fileName;
    private String fileType;
}
