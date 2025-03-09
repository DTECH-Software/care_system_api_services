/**
 * User: Himal_J
 * Date: 3/1/2025
 * Time: 10:13 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProfileImageUpdateRequestDTO extends ChannelRequestDTO{
    private String type;
    private String file;
}
