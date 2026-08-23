/**
 * User: Himal_J
 * Date: 2/11/2025
 * Time: 1:28 PM
 * <p>
 */

package com.dtech.message.dto.api;

import lombok.Data;

@Data
public class ITextMessageRequestDTO {
    private String to;
    private String text;
}
