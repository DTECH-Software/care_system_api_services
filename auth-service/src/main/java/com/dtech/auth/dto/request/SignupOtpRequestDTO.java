/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 8:03 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
@Data
public class SignupOtpRequestDTO extends ChannelRequestDTO{
    private String epfNo;
    private String nic;
    private String mobileNo;
}
