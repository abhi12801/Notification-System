-- Hibernate maps a java.lang.Integer field to INTEGER (int4) by default, not
-- SMALLINT (int2). Schema-validation correctly refused to start rather than
-- silently accept the width mismatch. Widening here instead of relying on a
-- narrower type Hibernate wasn't actually asking for; the CHECK constraints
-- (retry_count 0-3) already enforce the real bound, not the column width.
ALTER TABLE notifications ALTER COLUMN retry_count TYPE INTEGER;
ALTER TABLE notification_attempts ALTER COLUMN attempt_number TYPE INTEGER;
