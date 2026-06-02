# 22. Notification Message and Email

Status: Current business baseline for BA / QA / client review

## Purpose
Explain notification, SMS/message, and email behavior.

## Business Summary
The app uses in-app notifications, message-service/SMS, and email notifications for OTPs, approvals, rejections, and pending approval visibility.

## Main Business Rules
- Profile response includes unread notification count and latest notifications.
- OTP and selected claim messages are sent through message service.
- Dependent pending approval emails go to active same-company HR/admin users.
- Email recipient role codes are configured in auth-service logic.
- Claim final communication should use final L2/L3 remarks where required.

## BA Review Points
- Confirm email recipient roles.
- Confirm SMS events and message templates.
- Confirm notification read/unread behavior.

## QA Checkpoints
- Verify OTP message delivery path.
- Add dependent and verify pending approval email.
- Verify notification count after creating notifications.

## Client View
- Users and approvers should receive timely, relevant communication.

