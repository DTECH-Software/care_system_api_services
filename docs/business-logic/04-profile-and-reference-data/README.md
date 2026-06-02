# 04. Profile and Reference Data

Status: Current business baseline for BA / QA / client review

## Purpose
Explain profile retrieval and app reference data behavior.

## Business Summary
Profile and reference-data responses drive the app screens, available claim options, dependent options, and current balances.

## Main Business Rules
- Profile returns personal, address, company, policy, facility, and notification summary.
- Reference data returns treatments, categories, dependents, document limits, min past date, and fund limits.
- Reference data is user-specific.
- Dependent lists include only facility-eligible dependents.
- Balance values must align with dashboard and admin summary.

## BA Review Points
- Confirm what app screens need from profile and reference data.
- Confirm eligible dependent visibility.
- Confirm reference data refresh expectations.

## QA Checkpoints
- Compare reference-data balances with dashboard.
- Test users with INSURANCE, DEATH, and BOTH facility.
- Verify profile after company/staff category changes.

## Client View
- The app should show only options and balances that the user can actually use.

