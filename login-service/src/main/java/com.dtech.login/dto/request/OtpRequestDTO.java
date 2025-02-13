/**
 * User: Himal_J
 * Date: 2/13/2025
 * Time: 8:11 AM
 * <p>
 */

package com.dtech.login.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtpRequestDTO extends ChannelRequestDTO {
    private String otp;
}
