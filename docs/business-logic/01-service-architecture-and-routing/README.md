# 01. Service Architecture and Routing

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how Care App API services are separated and routed through gateway and registry services.

## Business Summary
Care App is split into separate Spring Boot services. Each service owns a business capability, while the cloud gateway and registry support external routing and service discovery.

## Main Business Rules
- Services are separated by capability: auth, login, token, claim, message, notification, document, cloud, and registry.
- Cloud/API gateway routes external requests to the correct service context path.
- Registry service supports service discovery where enabled.
- Business validation remains inside the owning service.
- Health endpoints are used for monitoring where exposed.

## BA Review Points
- Confirm which service owns each user-facing feature.
- Confirm gateway paths used by mobile and operational clients.
- Confirm whether direct service ports should remain accessible.

## QA Checkpoints
- Verify each service starts with the correct profile.
- Verify gateway routing reaches the expected service.
- Verify health endpoints work for monitoring.

## Client View
- Users interact with one app/API surface while business logic is handled by the correct backend service.
