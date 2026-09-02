/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:23 PM
 * <p>
 */

package com.dtech.login.dto.request.validator;


import com.dtech.login.enums.Channel;
import com.dtech.login.enums.Messages;
import com.dtech.login.validator.Conditional;
import com.dtech.login.validator.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Conditional(selected = "channel" , values = {"MB"} ,required = {"deviceDetails"} ,message = "Device details is required.")
@Conditional(selected = "message" , values = {"SIGN_IN","PASSWORD_RESET_REQUEST","PASSWORD_RESET","PASSWORD_RESET_OTP_VALIDATION","SIGN_OUT"} ,required = {"username"} ,message = "Username is required.")
public class ChannelRequestValidatorDTO {
    @NotBlank(message = "Channel is required.")
    @ValidEnum(enumClass = Channel.class,message = "Invalid channel.")
    private String channel;
    @NotBlank(message = "IP is required.")
    private String ip;
    @NotBlank(message = "Message is required.")
    @ValidEnum(enumClass = Messages.class, message = "Invalid message.")
    private String message;
    private String username;
    private String appVersion;
    @Valid
    private ChannelMbDeviceDetailsValidatorsDTO deviceDetails;
}
