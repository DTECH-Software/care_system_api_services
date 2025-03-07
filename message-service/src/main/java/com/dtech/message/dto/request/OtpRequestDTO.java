/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 11:25 AM
 * <p>
 */

package com.dtech.message.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpRequestDTO extends ChannelRequestDTO{
    private String primaryMobile;
    private SignupOtpRequestDTO signupOtp;
}
