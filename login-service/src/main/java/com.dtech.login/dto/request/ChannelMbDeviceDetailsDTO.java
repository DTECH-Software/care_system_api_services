/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 3:45 PM
 * <p>
 */

package com.dtech.login.dto.request;

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
