/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 2:04 PM
 * <p>
 */

package com.dtech.claim.dto.request.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelMbDeviceDetailsValidatorDTO {
    @NotBlank(message = "Device id is required.")
    private String deviceId;
    @NotBlank(message = "Model is required.")
    private String deviceModel;
    @NotBlank(message = "OS is required.")
    private String deviceOS;
    @NotBlank(message = "Name is required.")
    private String deviceName;
    @NotBlank(message = "Latitude is required.")
    private String latitude;
    @NotBlank(message = "Longitude is required.")
    private String longitude;
}
