/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 3:40 PM
 * <p>
 */

package com.dtech.auth.dto.response;

import lombok.Data;

@Data
public class DocumentResponseDTO {
    private String type;
    private byte[] doc;
}
