/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:55 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DocumentDownloadRequestDTO {
    private Long id;
    public DocumentDownloadRequestDTO(Long id){
        this.id = id;
    }
}
