/**
 * User: Himal_J
 * Date: 2/23/2025
 * Time: 7:52 PM
 * <p>
 */

package com.dtech.login.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;


@Data
public class ApplicationUserDetailsResponseDTO {
    private String accessToken;
    private String username;
    private String primaryEmail;
    private String primaryMobile;
    private boolean isReset;
    private String lastPasswordChangeDate;
    private String lastLoggedDate;
    private boolean expectingFirstTimeLogging;
    private boolean expectingDependentsRegister;
    private String passwordExpiredDate;
    private UserPersonalDetailsResponseDTO userPersonalDetails;
    private DocumentDownloadResponseDTO profileImg;
    private NotificationSummaryResponseDTO notification;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private Date createdDate;
}
