# 24. Docker Health and Uptime Monitoring

Status: Current business baseline for BA / QA / client review

## Purpose
Explain container runtime and monitoring expectations.

## Business Summary
Care App services can run in Docker and are monitored using health endpoints and Uptime Kuma.

## Main Business Rules
- Containers should use restart policy so they recover after reboot/failure.
- Memory limits and JVM options should match service sizing.
- Health checks should use /actuator/health where exposed.
- Health endpoint should return HTTP 200 with status UP.
- Uptime Kuma sends down and recovery emails to configured recipients.
- Maintenance mode should be used during planned deployments to avoid false alerts.

## BA Review Points
- Confirm alert recipients.
- Confirm maintenance procedure.
- Confirm health check interval.

## QA Checkpoints
- Stop a service and verify down alert.
- Restart service and verify recovery alert.
- Check Docker memory limit with docker inspect.

## Client View
- Operations team should know when services go down and when they recover.

