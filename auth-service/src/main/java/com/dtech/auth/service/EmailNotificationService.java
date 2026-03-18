package com.dtech.auth.service;

import com.dtech.auth.model.ApplicationUser;
import com.dtech.auth.model.ClaimsDependents;
import com.dtech.auth.model.CompanyTypes;
import com.dtech.auth.model.StaffCategories;
import com.dtech.auth.model.UserCompanyDetails;
import com.dtech.auth.model.UserPersonalDetails;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailNotificationService {

    @Autowired
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String mailFrom;

    public void notifyHrTeamOnDependentPendingApproval(List<String> recipientEmails,
                                                       ApplicationUser employee,
                                                       List<ClaimsDependents> dependents) {
        if (CollectionUtils.isEmpty(recipientEmails) || employee == null || CollectionUtils.isEmpty(dependents)) {
            log.info("Skipping dependent pending approval email - recipients or payload missing");
            return;
        }

        String subject = "Dependent Approval Required";
        String body = buildDependentPendingApprovalBody(employee, dependents);

        Set<String> processedEmails = new HashSet<>();
        for (String recipientEmail : recipientEmails) {
            sendHtmlMail(recipientEmail, subject, body, processedEmails);
        }
    }

    private void sendHtmlMail(String recipientEmail, String subject, String body, Set<String> processedEmails) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }

        String trimmedEmail = recipientEmail.trim();
        if (processedEmails != null && !processedEmails.add(trimmedEmail.toLowerCase(Locale.ROOT))) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (StringUtils.hasText(mailFrom)) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(trimmedEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Sent dependent pending approval email to {}", trimmedEmail);
        } catch (Exception ex) {
            log.error("Failed to send dependent pending approval email to {}", trimmedEmail, ex);
        }
    }

    private String buildDependentPendingApprovalBody(ApplicationUser employee, List<ClaimsDependents> dependents) {
        UserPersonalDetails personalDetails = employee.getUserPersonalDetails();
        UserCompanyDetails companyDetails = personalDetails != null ? personalDetails.getUserCompanyDetails() : null;
        CompanyTypes company = companyDetails != null ? companyDetails.getCompanyTypes() : null;
        StaffCategories staffCategory = companyDetails != null ? companyDetails.getStaffCategories() : null;

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #222;">
                    <p>Dear HR team,</p>
                    <p>A dependent has been added by the following employee and is pending your approval. The details are as follows:</p>
                    <p><strong>Employee details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold; width: 35%%;">Employee name</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Company</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">EPF number</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="border: 1px solid #d9d9d9; padding: 8px; font-weight: bold;">Staff category</td>
                            <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        </tr>
                    </table>
                    <p><strong>Dependent details</strong></p>
                    <table style="border-collapse: collapse; width: 100%%; max-width: 700px; margin-bottom: 16px;">
                        <tr>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Relationship</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Dependent name</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">Date of Birth</th>
                            <th style="border: 1px solid #d9d9d9; padding: 8px; text-align: left;">NIC (Above 16 years)</th>
                        </tr>
                        %s
                    </table>
                    <p>Please login to the WeCare system to continue the approval process.<br/>
                    <a href="https://wecare-admin.dsi.lk/care-admin">https://wecare-admin.dsi.lk/care-admin</a></p>
                    <p>This is an automated notification. Please do not reply to this email.</p>
                    <p>Regards,<br/>WeCare system<br/>Automated Notification</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(getEmployeeName(personalDetails)),
                escapeHtml(safeValue(company != null ? company.getDescription() : null)),
                escapeHtml(safeValue(personalDetails != null ? personalDetails.getEpfNo() : null)),
                escapeHtml(safeValue(staffCategory != null ? staffCategory.getDescription() : null)),
                buildDependentRows(dependents)
        );
    }

    private String buildDependentRows(List<ClaimsDependents> dependents) {
        StringBuilder rows = new StringBuilder();
        for (ClaimsDependents dependent : dependents) {
            rows.append("""
                    <tr>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                        <td style="border: 1px solid #d9d9d9; padding: 8px;">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(dependent.getRelationCategory() != null ? dependent.getRelationCategory().getDescription() : "-"),
                    escapeHtml(getDependentName(dependent)),
                    escapeHtml(formatDate(dependent.getDob())),
                    escapeHtml(safeValue(dependent.getNic()))
            ));
        }
        return rows.toString();
    }

    private String getEmployeeName(UserPersonalDetails employee) {
        if (employee == null) {
            return "N/A";
        }
        String firstName = StringUtils.hasText(employee.getFirstName()) ? employee.getFirstName().trim() : "";
        String lastName = StringUtils.hasText(employee.getLastName()) ? employee.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.hasText(fullName) ? fullName : "N/A";
    }

    private String getDependentName(ClaimsDependents dependent) {
        if (dependent == null) {
            return "N/A";
        }
        String firstName = StringUtils.hasText(dependent.getFirstName()) ? dependent.getFirstName().trim() : "";
        String lastName = StringUtils.hasText(dependent.getLastName()) ? dependent.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return StringUtils.hasText(fullName) ? fullName : "N/A";
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy/MM/dd").format(date);
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
