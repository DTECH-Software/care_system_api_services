# 07. Dependent Claim Eligibility

Status: Current business baseline for BA / QA / client review

## Purpose
Explain when a dependent can be selected for medical claims.

## Business Summary
Dependent claim eligibility is stricter than dependent registration. A dependent can be visible in profile but still blocked from claim creation because of approval status, facility, age, staff category, treatment, marital status, or previous death claim.

## Medical Claim Selection Rules
- The employee must be active and must have an assigned insurance policy.
- A dependent claim must reference a dependent that belongs to the same employee.
- The dependent must have status `APPROVED`.
- The dependent eligible facility must be `INSURANCE` or `BOTH`.
- If an approved death claim exists for the same employee and dependent, medical claim creation is blocked.
- Child dependent medical claims are blocked when child age is greater than 25.
- Normal Staff (`NS`) unmarried parent medical claims are blocked when parent age is greater than 65.
- Parent age exactly 65 is currently allowed because the code checks `age > 65`.
- Parent medical claims are fully blocked for `EX-OP1`, `EX-OP2`, `MM`, and `SNR`.
- Dependent CRIC restriction exists for staff codes `MM`, `EX-01`, and `EX-02` in current claim-service code.
- If the employee is unmarried and staff category is not `NS`, dependent medical claims are blocked.

## Parent Claim Rules
- Direct parent claim logic uses dependent category `PARENTS`, normally from `FATHER` or `MOTHER` relation.
- Normal Staff unmarried employee can claim for parent only while parent age is 65 or below.
- Parent age is calculated from dependent date of birth.
- If parent age is 66 or above, the claim request is blocked.
- If parent age is exactly 65, the current code allows the claim.
- For `EX-OP1`, `EX-OP2`, `MM`, and `SNR`, parent medical claims are fully blocked regardless of parent age.
- In-law relations are not the same as direct parent dependent category in the current implementation and need separate BA confirmation if the business wants to block them too.

## Child Claim Rules
- Child medical claim logic uses dependent category `CHILDREN`.
- Child age is calculated from dependent date of birth.
- If child age is greater than 25, claim creation is blocked.
- If child age is exactly 25, the current code allows the claim.

## Admin Approval Revalidation
- Care Admin revalidates key dependent rules before approving a claim.
- Dependent CRIC claims are blocked in admin approval.
- The dependent must still be approved and facility-eligible for `INSURANCE` or `BOTH`.
- Parent claims for `EX-OP1`, `EX-OP2`, `MM`, and `SNR` are blocked in admin approval.
- Approved dependent death claim blocks admin approval of a later medical claim.
- Current admin approval does not duplicate every age check; age-based blocking is mainly handled at Care App claim creation and scheduled facility recheck.

## Death Claim Impact
- Dependent death claim approval sets dependent live status to false in Care Admin.
- Approved dependent death claim also blocks later medical dependent claims.
- Rejected and under-review death claims do not block by themselves unless another approved death claim exists.

## BA Review Points
- Confirm parent restrictions for EX-OP1, EX-OP2, MM, and SNR.
- Confirm whether Normal Staff parent age should block above 65 only, or 65 and above.
- Confirm in-law handling.
- Confirm whether admin approval must re-check child and Normal Staff parent age even for old submitted claims.

## QA Checkpoints
- Submit parent claims for each restricted staff category.
- Submit Normal Staff unmarried parent claim with age 65 and verify current behavior allows it.
- Submit Normal Staff unmarried parent claim with age 66 and verify rejection.
- Test child over age 25.
- Approve dependent death claim and verify later medical claim is blocked.
- Try dependent CRIC claim for restricted staff codes.

## Client View
- Staff categories without parent cover should not be able to claim for parents.
