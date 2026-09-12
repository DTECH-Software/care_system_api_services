/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 1:59 PM
 * <p>
 */

package com.dtech.document.dto.request;

import lombok.Data;

@Data
public class DocumentDownloadRequestDTO{
    private String channel;
    private String appVersion;
    private String platform;
    private Long id;
    private boolean state;
}
