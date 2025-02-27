/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 11:36 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DependentRequestDTO extends ChannelRequestDTO {
    private List<DependentDetailsRequestDTO> dependents;
}
