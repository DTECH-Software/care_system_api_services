/**
 * User: Himal_J
 * Date: 3/6/2025
 * Time: 8:05 AM
 * <p>
 */

package com.dtech.claim.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class OtpRequestDTO extends ChannelRequestDTO{
    private String primaryMobile;
}
