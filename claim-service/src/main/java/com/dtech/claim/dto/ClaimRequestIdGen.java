/**
 * User: Himal_J
 * Date: 3/2/2025
 * Time: 7:04 PM
 * <p>
 */

package com.dtech.claim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class ClaimRequestIdGen {
    private String staffCategory;
    private String year;
    private String company;
}
