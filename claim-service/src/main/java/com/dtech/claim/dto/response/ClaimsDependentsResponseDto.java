/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 11:11 AM
 * <p>
 */

package com.dtech.claim.dto.response;


import lombok.Data;


@Data
public class ClaimsDependentsResponseDto {
    private Long id;
    private String initials;
    private String firstName;
    private String lastName;
    private String nic;
}
