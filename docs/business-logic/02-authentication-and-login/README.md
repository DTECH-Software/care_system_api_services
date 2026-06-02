# 02. Authentication and Login

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how mobile and web app users authenticate before using Care App APIs.

## Business Summary
Login validates active application user identity and returns app-facing profile, facility, token, and password/dependent registration flags.

## Main Business Rules
- Login requires an active application user.
- Username or primary email can be used where repository lookup supports it.
- Inactive, deleted, or missing users must not receive access.
- Successful login returns token, profile summary, facility, notification count, and password expiry.
- First-time/dependent-registration flags guide app next steps.

## BA Review Points
- Confirm login by username vs email.
- Confirm inactive and deleted user handling.
- Confirm fields needed by mobile after login.

## QA Checkpoints
- Test valid, invalid, inactive, and deleted users.
- Verify token and password expiry fields.
- Verify facility and dependent registration flags.

## Client View
- Only valid active users should access Care App features.

