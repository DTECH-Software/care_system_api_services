/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:23 PM
 * <p>
 */

package com.dtech.auth.dto.request.validator;


import com.dtech.auth.enums.Channel;
import com.dtech.auth.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChannelRequestValidatorDTO {
    @NotBlank(message = "Channel is required.")
    @ValidEnum(enumClass = Channel.class,message = "Invalid channel.")
    private String channel;
    @NotBlank(message = "IP is required.")
    private String ip;
    @NotBlank(message = "Username is required.")
    private String username;
    private String browser;
    private String deviceOS;
    private String deviceModel;
}
