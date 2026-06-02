# 09. Medical Claim Reference Data

Status: Current business baseline for BA / QA / client review

## Purpose
Explain the claim reference-data endpoint.

## Business Summary
Reference data prepares the app claim request screen with treatments, categories, eligible dependents, document limits, date limits, and balances.

## Main Business Rules
- Returns active treatments and treatment categories for the employee policy.
- Returns eligible dependents and allowed image/document counts.
- Returns insurance minimum past date from common parameter rules.
- Returns fundLimit and availableLimit by treatment and category.
- Includes rejoin and staff-category carry-forward where applicable.

## BA Review Points
- Confirm all treatments required by mobile are returned.
- Confirm dependent visibility per facility.
- Confirm balance consistency with dashboard.

## QA Checkpoints
- Test Normal Staff, EX-OP, MM, and SNR users.
- Compare reference-data balances with dashboard and admin summary.
- Test after promotion/rejoin.

## Client View
- Users should see correct available balances before submitting claims.

