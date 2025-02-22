/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 2:48 PM
 * <p>
 */

package com.dtech.login.dto.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChannelRequestDTO {
    private String channel;
    private String ip;
    private String username;
    private ChannelMbDeviceDetailsDTO deviceDetails;
}
