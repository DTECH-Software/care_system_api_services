/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 10:29 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ViewDependentRequestDTO extends ChannelRequestDTO{
    private Long id;
}
