-- A user account is either active or blocked: "Tạm ngưng" (INACTIVE) was the same thing, so it folds into BLOCKED.
UPDATE app_user SET status = 'BLOCKED' WHERE status = 'INACTIVE';
