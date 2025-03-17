/**
 * User: Himal_J
 * Date: 2/26/2025
 * Time: 8:08 AM
 * <p>
 */

package com.dtech.auth.dto.request.validator;

import com.dtech.auth.enums.DependentCategory;
import com.dtech.auth.enums.Gender;
import com.dtech.auth.enums.RelationCategory;
import com.dtech.auth.enums.Title;
import com.dtech.auth.validator.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Conditional(selected = "relationCategory" ,values = {"MOTHER","FATHER","WIFE","HUSBAND","FATHER_IN_LAW","MOTHER_IN_LAW"},required = {"jobTitle"},message = "Job title is required.")
@Conditional(selected = "relationCategory" ,values = {"MOTHER","FATHER","WIFE","HUSBAND","FATHER_IN_LAW","MOTHER_IN_LAW"},required = {"nic"},message = "NIC is required.")
@Conditional(selected = "relationCategory" ,values = {"WIFE","HUSBAND","FATHER_IN_LAW","MOTHER_IN_LAW","CHILD"},required = {"married"},message = "Married is required.")
@NicRequiredIfAge(message = "NIC is required for dependents aged 18 or older.")
public class ClaimDependentDetailsRequestValidatorDTO {
    @NotBlank(message = "Dependent category is required.")
    @ValidEnum(enumClass = DependentCategory.class,message = "Invalid dependent category.")
    private String dependentCategory;
    @NotBlank(message = "Title is required.")
    @ValidEnum(enumClass = Title.class,message = "Invalid title.")
    private String title;
    @NotBlank(message = "Initials is required.")
    private String initials;
    @NotBlank(message = "First name is required.")
    private String firstName;
    @NotBlank(message = "Last name is required.")
    private String lastName;
    @NotNull(message = "DOB is required.")
    @ValidAge(message = "Age must be below 65.")
    @ValidPastDays(message = "DOB must be past date")
    private Date dob;
    @NotBlank(message = "Gender is required.")
    @ValidEnum(enumClass = Gender.class,message = "Invalid gender type.")
    private String gender;
    private String jobTitle;
    @Pattern(regexp = "^[0-9]{9}[Vv]?$|^[0-9]{12}$", message = "Invalid NIC number. It must be 9 digits optionally followed by 'V' or 'v', or exactly 12 digits.")
    private String nic;
    @NotNull(message = "Relation category is required.")
    @ValidEnum(enumClass = RelationCategory.class,message = "Invalid relation category.")
    private String relationCategory;
    private String married;
    @NotNull(message = "Supporting document is required.")
    @NotEmpty(message = "Supporting document is required.")
    @Valid
    private List<SupportingDocumentValidatorDTO> documents;
}
