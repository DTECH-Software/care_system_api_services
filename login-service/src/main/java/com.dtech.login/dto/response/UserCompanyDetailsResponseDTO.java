/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:54 PM
 * <p>
 */

package com.dtech.login.dto.response;


import com.dtech.login.dto.SimpleBaseDTO;
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
    private Date terminateDate;
    private String designation;
    private SimpleBaseDTO insurancePolicy;
}
