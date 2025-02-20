/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 11:24 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;

@Data
public class SignupInquiryDTO extends ChannelRequestDTO{
    private String epfNo;
    private String nic;
}
