# 15. Rejoin and Promotion Carry Forward

Status: Current business baseline for BA / QA / client review

## Purpose
Explain balance carry-forward during rejoin and staff category change.

## Business Summary
When users rejoin or move between staff categories, previous approved usage can affect the new available balance.

## Main Business Rules
- Rejoin can carry approved utilization from the previous employee profile.
- Promotion can carry utilization from previous staff category into current category where rules require.
- Deleted duplicate profiles should not be treated as active carry-forward sources.
- Previous period/category is resolved from claim history and staff category period context.
- Dashboard, reference data, and admin summary should remain aligned.

## BA Review Points
- Confirm carry-forward for EX-OP1 to MM and MM to SNR.
- Confirm whether all previous categories carry to new category.
- Confirm duplicate inactive/deleted employee handling.

## QA Checkpoints
- Test rejoin user with previous approved claims.
- Test promotion in third quarter.
- Compare app and admin balances.

## Client View
- Balance should reflect the employee's full eligible claim history, not only the latest profile row.

