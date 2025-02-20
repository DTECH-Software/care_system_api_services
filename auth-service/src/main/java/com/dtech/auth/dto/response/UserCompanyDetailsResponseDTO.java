/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:54 PM
 * <p>
 */

package com.dtech.auth.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsResponseDTO {
    private CompanyTypesResponseDTO companyType;
    private StaffCategoryResponseDTO staffCategory;
    private StaffTypeResponseDTO staffType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date permanentDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date terminateDate;
}
