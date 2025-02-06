/**
 * User: Himal_J
 * Date: 2/5/2025
 * Time: 4:54 PM
 * <p>
 */

package com.dtech.login.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResetPasswordDTO extends ChannelRequestDTO {
    private String newPassword;
    private String confirmPassword;
}
