# 08. Employee Claim Eligibility

Status: Current business baseline for BA / QA / client review

## Purpose
Explain when an employee can submit a claim.

## Business Summary
Employee eligibility depends on active user status, facility, assigned policy, staff category, age, and treatment type.

## Main Business Rules
- Temporary users and DEATH-only users cannot submit medical claims.
- User must have an active insurance policy.
- Employee age limits apply by staff category and treatment type.
- Restricted treatments can have extra age/facility checks.
- Facility BOTH allows both medical and death claim flows where other rules pass.

## BA Review Points
- Confirm staff-category age limits.
- Confirm treatment-specific restrictions.
- Confirm temporary employee behavior.

## QA Checkpoints
- Test INSURANCE, DEATH, and BOTH facility users.
- Test age boundary cases.
- Test missing policy scenario.

## Client View
- The app should show and allow only claim options the employee is eligible to use.

