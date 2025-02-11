/**
 * User: Himal_J
 * Date: 2/10/2025
 * Time: 4:32 PM
 * <p>
 */

package com.dtech.login.dto.request;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequestDTO extends ChannelRequestDTO{
    private String mobileNo;
    private String type;
    private String value;
}
