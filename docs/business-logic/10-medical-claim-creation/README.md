# 10. Medical Claim Creation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain Care App medical claim submission.

## Business Summary
Medical claim creation validates employee/dependent eligibility, treatment, dates, documents, policy, and available balance before creating an under-review claim.

## Main Business Rules
- Claim may be for employee or approved dependent.
- User, facility, policy, treatment, category, date, amount, and documents are validated.
- Invalid dependent eligibility blocks claim creation.
- Claim is created in UNDER_REVIEW state.
- Initial approval workflow is created for L1 review.

## BA Review Points
- Confirm exact fields mandatory for each treatment.
- Confirm dependent and employee claim differences.
- Confirm front-end messages for validation failures.

## QA Checkpoints
- Submit employee and dependent claims.
- Test invalid policy/date/document/amount scenarios.
- Verify created claim status and workflow.

## Client View
- Claim creation should only succeed when the claim is eligible and complete.

