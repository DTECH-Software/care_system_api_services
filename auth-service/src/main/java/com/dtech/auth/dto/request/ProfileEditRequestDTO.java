/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 4:42 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileEditRequestDTO extends ChannelRequestDTO{
    private String primaryMobile;
    private String primaryEmail;
    private String otp;
}
