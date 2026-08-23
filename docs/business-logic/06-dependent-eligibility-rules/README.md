# 06. Dependent Eligibility Rules

Status: Current business baseline for BA / QA / client review

## Purpose
Explain dependent master validation rules.

## Business Summary
Dependent records are controlled by employee marital status, employee gender, relation category, dependent category, duplicate checks, supporting documents, approval status, eligible facility, and live status.

## Creation Rules
- Dependents can be added only for an active application user.
- Dependents created from Care App are saved for approval and are not claimable until approved by Care Admin.
- Unmarried employees cannot add `WIFE`, `HUSBAND`, `FATHER_IN_LAW`, `MOTHER_IN_LAW`, or `CHILD`.
- Male employees cannot add relation `HUSBAND`.
- Female employees cannot add relation `WIFE`.
- `HUSBAND`, `FATHER`, `BROTHER`, and `FATHER_IN_LAW` must not be submitted as female dependents.
- `WIFE`, `MOTHER`, `SISTER`, and `MOTHER_IN_LAW` must not be submitted as male dependents.
- A submitted `MOTHER` or `FATHER` must be older than the employee. Registration is blocked when the parent's completed age is equal to or less than the employee's completed age.

## Duplicate Rules
- `MOTHER` and `FATHER` are blocked when an `APPROVED` or `UNDER_REVIEW` record already exists for the same user and relation.
- `WIFE`, `HUSBAND`, `FATHER_IN_LAW`, and `MOTHER_IN_LAW` are blocked when an `APPROVED` or `UNDER_REVIEW` record already exists for the same user, relation, and married-round id.

## Document Rules
- `WIFE` and `HUSBAND` require exactly 2 documents: `BIRTH` and `MARRIED`.
- `MOTHER`, `FATHER`, `CHILD`, `BROTHER`, and `SISTER` require exactly 1 document: `BIRTH`.
- Incorrect document count or missing required document blocks dependent creation.

## Eligible Facility Rules
- Married employee: dependent category `SPOUSE` or `CHILDREN` is saved as eligible facility `BOTH`.
- Unmarried employee: relation `FATHER` or `MOTHER` is saved as eligible facility `BOTH`.
- Other dependents are saved as eligible facility `DEATH`.
- Medical claims can use dependents only when eligible facility is `INSURANCE` or `BOTH`.
- Death claims can use dependents when eligible facility is `DEATH` or `BOTH`.

## Live Status And Scheduled Recheck
- New dependents are saved with `liveStatus = true`.
- Approved dependent death claim changes live status to false through admin approval.
- Care App scheduled recheck changes eligible facility to `DEATH` for child age greater than 25, Normal Staff unmarried parent age greater than 65, and non-Normal-Staff dependent age greater than 70.
- The age implementation uses `age > limit`, so the exact limit age is allowed by the current code.

## BA Review Points
- Confirm married/unmarried relation restrictions.
- Confirm duplicate handling by relation.
- Confirm live status meaning after death claim approval.
- Confirm whether exact age limits should remain allowed or should be blocked.

## QA Checkpoints
- Test duplicate parents and spouse rounds.
- Test gender mismatch for relation category.
- Test live-status behavior after dependent death approval.
- Test child age 25/26, parent age 65/66, and non-NS dependent age 70/71.

## Client View
- The app should prevent invalid dependent records before approval.
