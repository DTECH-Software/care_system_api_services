/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:54 PM
 * <p>
 */

package com.dtech.auth.dto.response;


import com.dtech.auth.dto.SimpleBaseDTO;
import com.dtech.auth.enums.Facility;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsResponseDTO {
    private SimpleBaseDTO companyTypes;
    private SimpleBaseDTO staffCategories;
    private SimpleBaseDTO staffTypes;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private Date permanentDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private Date previousPermanentDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private Date transferDate;
    private Date terminateDate;
    private String designation;
    private SimpleBaseDTO insurancePolicy;
    private String facility;
    private String facilityDescription;
}
