/**
 * User: Himal_J
 * Date: 4/9/2025
 * Time: 3:22 PM
 * <p>
 */

package com.dtech.claim.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DetailsViewRequestDTO extends ChannelRequestDTO{
    private Long id;
}
