# 20. Death Dependent Live Status

Status: Current business baseline for BA / QA / client review

## Purpose
Explain dependent live-status changes after death claim approval.

## Business Summary
When a dependent death claim is finally approved, the dependent should no longer be treated as live/claimable.

## Main Business Rules
- Live status changes only after APPROVED dependent death claim.
- REJECTED and UNDER_REVIEW death claims do not change live status.
- Inactive/deceased dependent should not be eligible for future medical dependent claims.
- The update should be traceable from approval history.
- Admin and app behavior must use the same dependent live-status meaning.

## BA Review Points
- Confirm whether liveStatus false is the required value after approval.
- Confirm if historical claims remain visible.
- Confirm if manual reversal is allowed.

## QA Checkpoints
- Approve dependent death claim and verify liveStatus.
- Reject dependent death claim and verify no liveStatus change.
- Try medical claim for deceased dependent.

## Client View
- Once dependent death is approved, the dependent should not be used for future claims.

