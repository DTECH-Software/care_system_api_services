/**
 * User: Himal_J
 * Date: 3/7/2025
 * Time: 1:30 PM
 * <p>
 */

package com.dtech.message.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpValidationDTO extends ChannelRequestDTO {
    private String otp;
}
