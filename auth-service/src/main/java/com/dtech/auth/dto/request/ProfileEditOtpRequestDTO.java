/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 3:29 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileEditOtpRequestDTO extends ChannelRequestDTO{
    private String primaryMobile;
}
