# 16. Claim Request ID Generation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain medical and death claim request number generation.

## Business Summary
Request IDs provide readable business identifiers for claims and differ by claim type, company, staff category, policy year, and sequence.

## Main Business Rules
- Medical request IDs include healthcare prefix, company, staff category, year, and sequence.
- Death claim IDs include DDF context and relevant company/staff grouping.
- Sequence increments by matching business grouping.
- Promotion or staff category change can change the staff code used in request ID.
- Request IDs must remain unique and readable.

## BA Review Points
- Confirm format for each claim type.
- Confirm sequence reset period.
- Confirm company and staff category source.

## QA Checkpoints
- Create multiple claims in same group and verify sequence.
- Test different company/staff categories.
- Test DDF request ID format.

## Client View
- Claim request ID should be stable and easy to reference in support and approval.

