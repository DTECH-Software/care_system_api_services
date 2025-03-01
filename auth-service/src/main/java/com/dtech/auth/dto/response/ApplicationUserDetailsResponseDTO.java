/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 7:52 PM
 * <p>
 */

package com.dtech.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ApplicationUserDetailsResponseDTO {
    private String username;
    private String primaryEmail;
    private String primaryMobile;
    private boolean isReset;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastPasswordChangeDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoggedDate;
    private boolean expectingFirstTimeLogging;
    private boolean mbExpectingFirstTimeLogging;
    private boolean expectingDependentsRegister;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date passwordExpiredDate;
    private UserPersonalDetailsResponseDTO userPersonalDetails;
    private DocumentResponseDTO profileImg;
}
