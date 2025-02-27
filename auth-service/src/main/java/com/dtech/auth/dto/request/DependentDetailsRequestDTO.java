/**
 * User: Himal_J
 * Date: 2/25/2025
 * Time: 11:37 AM
 * <p>
 */

package com.dtech.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class DependentDetailsRequestDTO {
    private String dependentCategory;
    private String initials;
    private String firstName;
    private String lastName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String dob;
    private String gender;
    private String jobTitle;
    private String nic;
    private String relationCategory;
    private List<SupportingDocumentDTO> document;
}
