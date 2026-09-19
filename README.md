# Product

We build a purchase request tracker. People track purchase requests from drafting through approval to ordering.

This repository is used for the whole semester. Lab 01 is a small Java 21 domain model built with Maven, without Spring or a database. The starter was created from scratch because the course starter was not available.

## Core item

The core item is a purchase request for goods that must be approved before an order can be placed.

- `PurchaseId` is its immutable, non-null, non-blank identifier, for example `PUR-001`. Invalid IDs throw `IllegalArgumentException`.
- `PurchaseStatus` has three values: `DRAFT`, `APPROVED`, and `ORDERED`.
- `PurchasePolicy.move(from, to)` returns the target status for an allowed change and throws `IllegalStateException` for every other change between valid statuses. Missing statuses throw `NullPointerException`.

Only `DRAFT -> APPROVED` and `APPROVED -> ORDERED` are allowed. `ORDERED` is terminal, and moving to the same status is not a status change.

Requirements: **JDK 21** and **Maven 3.9 or later**. Run these commands from the repository root:

```sh
java -version
mvn -version
mvn -q test
```

`java -version` must report version 21; Maven must also use Java 21. A successful `mvn -q test` may print nothing and exits with code 0. Detailed test results are in `target/surefire-reports/`. GitHub Actions runs the same test command on Java 21.

`PurchasePolicyTest` contains the same four status-table rows in one `@CsvSource`. Additional tests cover the other forbidden transitions, missing statuses, and valid/null/empty/whitespace-only IDs.

## Status table

| From | To | Result | Business reason |
| --- | --- | --- | --- |
| `DRAFT` | `APPROVED` | Allowed | The manager approves the purchase request. |
| `APPROVED` | `ORDERED` | Allowed | The approved purchase request can be sent to a supplier. |
| `DRAFT` | `ORDERED` | Forbidden | Ordering would skip mandatory manager approval. |
| `ORDERED` | `DRAFT` | Forbidden | An order already sent to a supplier cannot be reopened as a draft. |

## Forbidden — why

1. **`DRAFT -> ORDERED`:** the request has not been approved. Allowing this change would bypass spending approval.
2. **`ORDERED -> DRAFT`:** the supplier has already received the order. Returning it to a draft would hide that commitment; a cancellation must be handled as a separate process outside this lab.

All unlisted changes are also rejected. This first version supports only forward progress through approval and ordering.
