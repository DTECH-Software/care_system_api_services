/**
 * User: Himal_J
 * Date: 2/24/2025
 * Time: 8:36 AM
 * <p>
 */

package com.dtech.message.dto.request;

import lombok.Data;

@Data
public class ChannelMbDeviceDetailsDTO {
    private String deviceId;
    private String deviceModel;
    private String deviceOS;
    private String deviceName;
    private String latitude;
    private String longitude;
}
