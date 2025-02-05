/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 2:48 PM
 * <p>
 */

package com.dtech.token.dto.request;

import lombok.Data;

@Data
public class ChannelRequestDTO {
    private String channel;
    private String ip;
    private String username;
    private String browser;
    private String deviceOS;
    private String deviceModel;
}
