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
    private AmountResponseDTO indoor;
    private AmountResponseDTO outdoor;
    private AmountResponseDTO critical;
    private AmountResponseDTO death;

    public CountTypeResponseDTO(Long fullCount, Long approvedCount, Long rejectedCount, Long underReviewCount) {
        this.fullCount = fullCount;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.underReviewCount = underReviewCount;
    }

//    public CountTypeResponseDTO(Long fullCount, Long approvedCount, Long rejectedCount, Long underReviewCount) {
//        System.out.println("CountTypeResponseDTO constructor called death!");
//        this.fullCount = fullCount;
//        this.approvedCount = approvedCount;
//        this.rejectedCount = rejectedCount;
//        this.underReviewCount = underReviewCount;
//    }

}
