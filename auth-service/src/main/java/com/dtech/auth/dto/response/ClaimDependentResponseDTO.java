/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 10:36 AM
 * <p>
 */

package com.dtech.auth.dto.response;


import lombok.Data;

import java.util.List;

@Data
public class ClaimDependentResponseDTO {
    private List<ClaimDependentDetailsResponseDTO> dependents;
}
