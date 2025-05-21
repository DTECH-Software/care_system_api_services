/**
 * User: Himal_J
 * Date: 2/28/2025
 * Time: 10:39 AM
 * <p>
 */

package com.dtech.auth.dto.response;

import com.dtech.auth.dto.SimpleBaseDTO;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ClaimDependentDetailsResponseDTO {
    private Long id;
    private String dependentCategory;
    private String dependentCategoryDescription;
    private String title;
    private String titleDescription;
    private String initials;
    private String firstName;
    private String lastName;
    private Date dob;
    private String gender;
    private String genderDescription;
    private String nic;
    private String jobTitle;
    private String relationCategory;
    private String relationCategoryDescription;
    private String status;
    private String statusDescription;
    private SimpleBaseDTO married;
    private Boolean liveStatus;
    private String eligibleFacility;
    private int age;
    private Date createdDate;
    private List<DocumentDownloadResponseDTO> documents;
}
