/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:52 PM
 * <p>
 */

package com.dtech.auth.dto.response;

import lombok.Data;

@Data
public class UserAddressResponseDTO {
    private String streetNo;
    private String street1;
    private String street2;
    private String city;
}
