# 18. Dashboard Summary

Status: Current business baseline for BA / QA / client review

## Purpose
Explain dashboard counts, lists, balances, and active year.

## Business Summary
Dashboard summary combines claim counts, latest claim lists, and treatment-level remaining balances for the logged-in user.

## Main Business Rules
- Counts are grouped by APPROVED, REJECTED, and UNDER_REVIEW.
- Latest approved, rejected, and under-review claims are returned.
- Balance totals use fund limit minus approved utilization.
- Active year is resolved from selected/requested year and current policy period.
- Different request year values can produce different dashboard results.

## BA Review Points
- Confirm year filter behavior for mobile and OP channels.
- Confirm whether rejected claims reduce balance.
- Confirm dashboard vs reference-data balance consistency.

## QA Checkpoints
- Compare MB and OP payloads with the same year.
- Test approved/rejected/under-review counts.
- Verify activeYear in response.

## Client View
- Dashboard should reflect the selected policy year and current claim status.

