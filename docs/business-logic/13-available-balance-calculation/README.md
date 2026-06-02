# 13. Available Balance Calculation

Status: Current business baseline for BA / QA / client review

## Purpose
Explain fundLimit and availableLimit calculation.

## Business Summary
Available balance is the policy limit remaining after approved utilization and required carry-forward usage are applied.

## Main Business Rules
- Fund limit is selected from policy, treatment, category, period, and quarter.
- Available limit equals fund limit minus approved utilization.
- Approved claims reduce available balance.
- Rejected claims with zero approved amount do not reduce available balance.
- Carry-forward usage from rejoin or promotion can reduce current balance.

## BA Review Points
- Confirm whether under-review claims should reserve balance.
- Confirm carry-forward scenarios.
- Confirm shared bucket vs category bucket behavior.

## QA Checkpoints
- Compare dashboard and reference-data balances.
- Test rejected and partial approved claims.
- Test rejoin and promotion balances.

## Client View
- Displayed available balance should match what the user can actually claim.

