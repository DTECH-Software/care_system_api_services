# 21. Mobile and OP Channel Behavior

Status: Current business baseline for BA / QA / client review

## Purpose
Explain channel-specific request context.

## Business Summary
Mobile and OP clients can call the same endpoints. Business results should depend on user/year/filter data, not just channel.

## Main Business Rules
- Mobile requests use channel MB and can include device details.
- OP requests use channel OP and commonly include IP, userAgent, filters, and pagination.
- If MB and OP send different year/filter values, responses can legitimately differ.
- Channel must not bypass eligibility, balance, or workflow rules.
- Logs should preserve channel and request metadata.

## BA Review Points
- Confirm default year behavior for mobile.
- Confirm whether OP and MB should use same payload rules.
- Confirm device details usage.

## QA Checkpoints
- Compare MB and OP with same username/year.
- Test missing or different year values.
- Verify channel does not bypass validation.

## Client View
- Same input should produce same business result regardless of client channel.

