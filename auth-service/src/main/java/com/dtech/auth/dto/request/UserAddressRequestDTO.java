/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 6:29 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;

@Data
public class UserAddressRequestDTO {
    private String streetNo;
    private String street1;
    private String street2;
    private String city;
}
