# 23. Document Cloud Registry and Token Integration

Status: Current business baseline for BA / QA / client review

## Purpose
Explain cross-service integration rules used by Care App APIs.

## Business Summary
Care App services depend on document upload, cloud gateway routing, registry discovery, and token validation to support business operations.

## Main Business Rules
- Document service stores uploaded claim/dependent documents and returns metadata.
- Cloud gateway routes external requests to the correct service.
- Registry service supports discovery where enabled.
- Token service supports token creation/validation where used.
- Integration failure should return controlled errors and logs.

## BA Review Points
- Confirm which services are mandatory for each feature.
- Confirm whether direct service access is allowed.
- Confirm error wording for integration failures.

## QA Checkpoints
- Upload documents through dependent and claim flows.
- Test gateway route to each service.
- Test protected endpoint with invalid token.

## Client View
- Service integrations should be invisible to users except when a clear business-safe error is needed.

