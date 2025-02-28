/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 11:37 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ClaimDependentDetailsRequestDTO {
    private String dependentCategory;
    private String initials;
    private String firstName;
    private String lastName;
    private Date dob;
    private String gender;
    private String jobTitle;
    private String nic;
    private String relationCategory;
    private List<SupportingDocumentDTO> documents;
}
