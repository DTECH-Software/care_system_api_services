/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:50 PM
 * <p>
 */

package com.dtech.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserPersonalDetailsResponseDTO {
    private String epfNo;
    private Boolean isTemp;
    private String tempId;
    private String initials;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String mobileNo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date dob;
    private long age;
    private String gender;
    private String genderDescription;
    private String title;
    private String titleDescription;
    private String maritalStatus;
    private String maritalStatusDescription;
    private UserAddressResponseDTO userAddress;
    private UserCompanyDetailsResponseDTO userCompanyDetails;
}
