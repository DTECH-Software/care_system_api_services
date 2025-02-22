/**
 * User: Himal_J
 * Date: 2/22/2025
 * Time: 6:30 PM
 * <p>
 */

package com.dtech.auth.dto.request;

import com.dtech.auth.dto.SimpleBaseDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsRequestDTO {
    private SimpleBaseDTO companyType;
    private SimpleBaseDTO staffCategory;
    private SimpleBaseDTO staffType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date permanentDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date terminateDate;
}
