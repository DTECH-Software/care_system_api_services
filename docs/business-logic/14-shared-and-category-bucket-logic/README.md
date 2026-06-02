# 14. Shared and Category Bucket Logic

Status: Current business baseline for BA / QA / client review

## Purpose
Explain how shared and category-specific limits behave.

## Business Summary
Some policies share one treatment-level bucket across categories; others have separate category limits with a global cap.

## Main Business Rules
- Shared buckets reduce sibling category availability from one treatment-level pool.
- Category-specific buckets reduce the selected category independently.
- Nested/global treatment limit can still cap category-specific availability.
- Normal Staff Outdoor can behave as a shared bucket where configured.
- Executive/management policies can have separate Dental, Spectacle, and Other category limits.

## BA Review Points
- Confirm shared bucket policies by staff category.
- Confirm category-specific policies by staff category.
- Confirm displayed balance when global cap is lower than category balance.

## QA Checkpoints
- Submit claims in different categories and verify sibling availability.
- Compare Normal Staff and Executive behavior.
- Test global cap edge cases.

## Client View
- Category balances should move according to the actual policy bucket structure.

