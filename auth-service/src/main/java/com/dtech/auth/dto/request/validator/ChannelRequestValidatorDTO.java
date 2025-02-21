/**
 * User: Himal_J
 * Date: 2/3/2025
 * Time: 12:23 PM
 * <p>
 */

package com.dtech.auth.dto.request.validator;


import com.dtech.auth.enums.Channel;
import com.dtech.auth.enums.Messages;
import com.dtech.auth.validator.UsernameRequiredIfMessage;
import com.dtech.auth.validator.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@UsernameRequiredIfMessage(messagesThatDontRequireUsername = {"OnboardingDetailsVerification","OnboardingOtpRequest","OnboardingOtpRequestValidation"}, message = "Username is required.")
public class ChannelRequestValidatorDTO {
    @NotBlank(message = "Channel is required.")
    @ValidEnum(enumClass = Channel.class,message = "Invalid channel.")
    private String channel;
    @NotBlank(message = "IP is required.")
    private String ip;
    @NotBlank(message = "Message is required.")
    @ValidEnum(enumClass = Messages.class,message = "Invalid message.")
    private String message;
    private String username;
    private String browser;
    private String deviceOS;
    private String deviceModel;
}
