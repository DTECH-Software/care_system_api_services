# 11. Claim Validation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain date, document, and amount validation for claim submission.

## Business Summary
Claim validation protects policy rules before a claim enters approval workflow.

## Main Business Rules
- Treatment date cannot be older than the configured claim request period.
- From date cannot be after to date where both are provided.
- Treatment dates must align with the applicable policy period.
- Treatment bill and diagnosis image counts must match configured min/max rules.
- Requested amount must fit the selected limit/balance rules.

## BA Review Points
- Confirm claim request period value.
- Confirm document count by treatment.
- Confirm amount/balance validation per staff category.

## QA Checkpoints
- Test old treatment date.
- Test missing/extra documents.
- Test amount above available balance.

## Client View
- The app should block invalid claims before they reach approvers.

