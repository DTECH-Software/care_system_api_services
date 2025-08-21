package com.dtech.claim.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountResponseDTO {
    private BigDecimal sumOfUtilizeAmount;
    private BigDecimal totalLimit;
    private BigDecimal remainingAmount;

    public AmountResponseDTO(BigDecimal sumOfUtilizeAmount, BigDecimal totalLimit, BigDecimal remainingAmount) {
        System.out.println("AmountResponseDTO constructor called!");
        this.sumOfUtilizeAmount = sumOfUtilizeAmount;
        this.totalLimit = totalLimit;
        this.remainingAmount = remainingAmount;
    }

    public AmountResponseDTO(BigDecimal sumOfUtilizeAmount) {
        System.out.println("AmountResponseDTO constructor called!");
        this.sumOfUtilizeAmount = sumOfUtilizeAmount;
    }
}
