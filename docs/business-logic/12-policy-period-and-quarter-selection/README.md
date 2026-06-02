# 12. Policy Period and Quarter Selection

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how active insurance period and quarter are selected.

## Business Summary
Policy period and quarter selection determine the correct fund limit for the employee's current staff category and effective permanent date.

## Main Business Rules
- Current staff category period is selected by date and staff category.
- Claim dates must fall inside the selected period.
- Effective permanent date determines quarter/prorated limit.
- Previous permanent date can be considered in rejoin/minimum-period scenarios.
- Missing period or quarter data returns controlled validation error.

## BA Review Points
- Confirm rejoin previous permanent date rule.
- Confirm first-quarter fallback behavior.
- Confirm period selection for promotion users.

## QA Checkpoints
- Test claim on period boundaries.
- Test rejoin with current and previous permanent dates.
- Test promotion in later quarter.

## Client View
- Users should receive the correct annual/prorated limit for their employment timeline.

