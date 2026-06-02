# 19. Death Claim Creation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain DDF/death donation claim request behavior.

## Business Summary
Death claim creation validates employee or dependent eligibility before creating a death donation workflow request.

## Main Business Rules
- User must have DEATH or BOTH facility where applicable.
- Dependent death claims require a valid approved dependent.
- Death claims start under review and follow death approval workflow.
- Death claim rules are separate from medical treatment category rules.
- Approved dependent death claim can update dependent live status.

## BA Review Points
- Confirm eligible relations for dependent death claims.
- Confirm death facility requirement.
- Confirm live-status update timing.

## QA Checkpoints
- Create employee and dependent death claims.
- Test DEATH-only and BOTH facility users.
- Verify approved dependent death claim live-status behavior.

## Client View
- Death claims should be allowed only for eligible users/dependents.

