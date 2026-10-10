\set ON_ERROR_STOP on

-- A fresh key allows repeatable runs without deleting previously saved purchases.
SELECT 'PR-ROLLBACK-' || gen_random_uuid()::text AS demo_key \gset

BEGIN;
INSERT INTO purchase_request (id, business_key, status, title)
VALUES (gen_random_uuid(), :'demo_key', 'DRAFT', 'Laptop for design team');

-- Only this statement is expected to fail. Capture its exact SQLSTATE.
\set ON_ERROR_STOP off
INSERT INTO purchase_request (id, business_key, status, title)
VALUES (gen_random_uuid(), :'demo_key', 'DRAFT', 'Duplicate laptop request');
\set duplicate_state :SQLSTATE
\set ON_ERROR_STOP on

ROLLBACK;

SELECT :'duplicate_state' = '23505' AS duplicate_detected,
       count(*) = 0 AS rolled_back
FROM purchase_request WHERE business_key = :'demo_key' \gset

\if :duplicate_detected
\else
    \echo 'FAIL: the second statement did not report SQLSTATE 23505'
    SELECT 1 / 0 AS expected_duplicate_key_violation;
\endif

\if :rolled_back
\else
    \echo 'FAIL: the transaction left a row'
    SELECT 1 / 0 AS expected_empty_transaction;
\endif

SELECT count(*) AS count_after_rollback
FROM purchase_request WHERE business_key = :'demo_key';
\echo 'PSQL ROLLBACK PASSED: SQLSTATE 23505; count = 0'
