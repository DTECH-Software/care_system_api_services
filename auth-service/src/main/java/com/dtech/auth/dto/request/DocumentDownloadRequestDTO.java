/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 12:55 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDownloadRequestDTO {
    private Long id;
    private boolean state;
}
