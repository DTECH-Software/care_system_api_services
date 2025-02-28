/**
 * User: Himal_J
 * Date: 2/20/2025
 * Time: 1:54 PM
 * <p>
 */

package com.dtech.auth.dto.response;


import com.dtech.auth.dto.SimpleBaseDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserCompanyDetailsResponseDTO {
    private SimpleBaseDTO companyTypes;
    private SimpleBaseDTO staffCategories;
    private SimpleBaseDTO staffTypes;
    private Date permanentDate;
    private Date terminateDate;
}
