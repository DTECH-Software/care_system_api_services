/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 1:50 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SupportingDocumentDTO {
    private String type;
    private MultipartFile doc;
}
