/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:23 PM
 * <p>
 */

package com.dtech.token.dto.request.validator;

import com.dtech.token.enums.Channel;
import com.dtech.token.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelRequestValidatorDTO {
    @NotBlank(message = "Channel is required.")
    @ValidEnum(enumClass = Channel.class,message = "Invalid channel.")
    private String channel;
    @NotBlank(message = "IP is required")
    private String ip;
    @NotBlank(message = "Username is required")
    private String username;
    private String browser;
    private String deviceOS;
    private String deviceModel;
}
