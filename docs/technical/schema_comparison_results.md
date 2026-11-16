# Đối chiếu schema local v2

Captured UTC: 2026-11-12T09:26:56.3758617+00:00

Source before/recheck: 26/26 public tables. Target: 34.

Approved additions: 8 tables (place_video removed at explicit user request on 11/11/2026), 10 columns (including per-quest points, description and settled reward snapshots), CHECKs on a new column and one partial UNIQUE index on an old table. New-table PK/FK/UNIQUE/index definitions are in the snapshots and approval SQL.

Known restore difference: journey_status_check distributes the varchar-array cast to individual text elements. The accepted status set remains IN_PROGRESS/COMPLETED/ABANDONED. Original and restored definitions remain available in both snapshots. No corrective ALTER was executed.

PASS: Source bcv schema AND per-table row counts equal the initial snapshot.
PASS: All old table/column types, NULL constraints, defaults, identity metadata, PK/FK/UNIQUE/CHECK/index definitions preserved, except the documented equivalent deparser representation.
PASS: Added tables/columns/constraints/indexes on old tables match the approved allowlist.
This comparison does not export personal rows or prove every newly added application behavior. New business tables were initially empty before target-only reference seeding and integration fixtures.