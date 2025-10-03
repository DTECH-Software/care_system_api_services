package com.dtech.claim.dto;

public interface InsuranceClaimCountProjection {
    Long getFullCount();
    Long getApprovedCount();
    Long getRejectedCount();
    Long getUnderReviewCount();
}
