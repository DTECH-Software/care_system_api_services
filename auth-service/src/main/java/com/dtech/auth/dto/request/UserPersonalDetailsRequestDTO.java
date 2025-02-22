/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 6:28 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserPersonalDetailsRequestDTO {
    private String username;
    private String password;
    private String confirmPassword;
    private String epfNo;
    private String initials;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dob;
    private UserAddressRequestDTO userAddress;
    private UserCompanyDetailsRequestDTO userCompanyDetails;
}
