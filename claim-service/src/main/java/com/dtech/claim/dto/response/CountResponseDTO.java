/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 8:56 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountResponseDTO {
    private CountTypeResponseDTO countDetails;
    private List<LatestUpdatedResponseDTO> approved;
    private List<LatestUpdatedResponseDTO> rejected;
    private List<LatestUpdatedResponseDTO> underReview;
}
