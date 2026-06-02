# 03. Password Reset and OTP

Status: Current business baseline for BA / QA / client review

## Purpose
Explain OTP and password reset behavior.

## Business Summary
OTP protects sensitive account operations such as password reset and contact/profile changes.

## Main Business Rules
- OTP is required before sensitive updates.
- OTP lookup checks username or primary email where supported.
- Invalid OTP blocks the requested action.
- Password expiry is calculated from password policy.
- Expiry display should avoid time confusion where date-only expiry is required.

## BA Review Points
- Confirm OTP expiry and retry limits.
- Confirm six-month password expiry expectation.
- Confirm date-only vs datetime display.

## QA Checkpoints
- Test valid OTP, invalid OTP, expired OTP, and repeated attempts.
- Verify password expiry after reset.
- Verify inactive users cannot reset as active users.

## Client View
- Users should complete secure verification before account-sensitive changes.

