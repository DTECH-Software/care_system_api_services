# 05. Dependent Creation and Documents

Status: Current business baseline for BA / QA / client review

## Purpose
Explain dependent registration and document requirements.

## Business Summary
Employees can submit dependents from the app, but dependents require validation, supporting documents, and admin approval before claim use.

## Main Business Rules
- Dependents are saved under the requesting active application user.
- New dependent records require admin approval.
- Spouse relations require birth and marriage documents.
- Parent, child, sibling, and similar relations require birth certificate documents.
- Document upload is handled through document-service integration.
- Dependent pending approval email is sent to same-company HR/admin users.

## BA Review Points
- Confirm document requirements by relation.
- Confirm pending approval email recipients.
- Confirm whether multiple dependents can be submitted together.

## QA Checkpoints
- Test missing documents by relation.
- Test successful upload and pending approval state.
- Verify email recipient company filtering.

## Client View
- Dependents should be registered with correct proof before they become claimable.

