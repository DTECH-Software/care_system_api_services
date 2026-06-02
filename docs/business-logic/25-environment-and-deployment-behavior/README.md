# 25. Environment and Deployment Behavior

Status: Current business baseline for BA / QA / client review

## Purpose
Explain environment profile and deployment behavior.

## Business Summary
Services run with environment-specific configuration and can be deployed by CI/CD as jars or Docker containers.

## Main Business Rules
- Services run with active profile such as uat, sit, or prod.
- Database, SMTP, service URLs, and security configuration can vary by profile.
- Docker compose files define container startup and exposed ports.
- GitHub Actions can build, copy, and restart services on the server.
- Production secrets must not be exposed in public docs or logs.

## BA Review Points
- Confirm target environment for each release.
- Confirm deployment downtime expectation.
- Confirm operational owner for failures.

## QA Checkpoints
- Verify active profile after deployment.
- Verify container status and health endpoints.
- Verify app API behavior after restart.

## Client View
- Deployments should be predictable and should preserve environment-specific behavior.

