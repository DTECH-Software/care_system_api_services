/**
 * User: Himal_J
 * Date: 3/28/2025
 * Time: 8:53 AM
 * <p>
 */

package com.dtech.claim.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CountTypeResponseDTO {
    private Long fullCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Long underReviewCount;
    private BigDecimal sumOfUtilizeAmount;
    private BigDecimal remainingAmount;

    public CountTypeResponseDTO(Long fullCount, Long approvedCount, Long rejectedCount, Long underReviewCount, BigDecimal sumOfUtilizeAmount, BigDecimal remainingAmount) {
        System.out.println("CountTypeResponseDTO constructor called!");
        this.fullCount = fullCount;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.underReviewCount = underReviewCount;
        this.sumOfUtilizeAmount = sumOfUtilizeAmount;
        this.remainingAmount = remainingAmount;
    }
    public CountTypeResponseDTO(Long fullCount, Long approvedCount, Long rejectedCount, Long underReviewCount, BigDecimal sumOfUtilizeAmount) {
        System.out.println("CountTypeResponseDTO constructor called!");
        this.fullCount = fullCount;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.underReviewCount = underReviewCount;
        this.sumOfUtilizeAmount = sumOfUtilizeAmount;
    }

}
