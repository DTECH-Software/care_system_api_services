/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 3:27 PM
 * <p>
 */

package com.dtech.token.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenValidResponseDTO {
    private boolean valid;
    private String username;
}
