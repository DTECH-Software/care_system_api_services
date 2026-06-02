# 17. Claim History and Status

Status: Current business baseline for BA / QA / client review

## Purpose
Explain claim history and status meaning shown to app users.

## Business Summary
Claim history lists the user's medical and death claims with their latest business status and core details.

## Main Business Rules
- UNDER_REVIEW means submitted and waiting for approval workflow completion.
- APPROVED means final approval workflow decision approved the claim.
- REJECTED means final workflow decision rejected the claim.
- Partial approval is APPROVED with approved amount lower than request amount.
- History remains visible for approved, rejected, and under-review claims.

## BA Review Points
- Confirm status wording shown to users.
- Confirm partial approval display.
- Confirm sorting order and pagination.

## QA Checkpoints
- Test each status.
- Test employee and dependent claims.
- Verify latest-first order.

## Client View
- Users should clearly see where each claim currently stands.

