/**
 * User: Himal_J
 * Date: 3/30/2025
 * Time: 10:00 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class LatestUpdatedResponseDTO {
    private Long id;
    private String requestId;
    private String treatment;
    private String remark;
    private String diagnosis;
    private BigDecimal amount;
    private String passion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestDate;

    public LatestUpdatedResponseDTO(Long id,String requestId, String treatment, String remark,
                                    String diagnosis, BigDecimal amount, String passion,
                                    Date requestDate) {
        this.id = id;
        this.requestId = requestId;
        this.treatment = treatment;
        this.remark = remark;
        this.diagnosis = diagnosis;
        this.amount = amount;
        this.passion = passion;
        this.requestDate = requestDate;
    }
}
