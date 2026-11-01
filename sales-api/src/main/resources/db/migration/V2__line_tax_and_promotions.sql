ALTER TABLE sale_lines ADD COLUMN tax_code        VARCHAR(20);
ALTER TABLE sale_lines ADD COLUMN tax_overridden  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sale_lines ADD COLUMN promotion_code  VARCHAR(30);
