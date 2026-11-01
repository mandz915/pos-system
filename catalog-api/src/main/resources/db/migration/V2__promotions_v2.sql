ALTER TABLE promotions ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE promotions DROP CONSTRAINT promotions_type_check;
ALTER TABLE promotions DROP CONSTRAINT promotions_value_check;
ALTER TABLE promotions ALTER COLUMN value DROP NOT NULL;
ALTER TABLE promotions ADD CONSTRAINT promotions_type_check CHECK (type IN ('PERCENT', 'FIXED', 'BUY_X_GET_Y'));
ALTER TABLE promotions ADD CONSTRAINT promotions_value_check
    CHECK (type = 'BUY_X_GET_Y' OR (value IS NOT NULL AND value > 0));

ALTER TABLE promotions ADD COLUMN buy_qty     INT;
ALTER TABLE promotions ADD COLUMN get_qty     INT;
ALTER TABLE promotions ADD COLUMN min_qty     INT;
ALTER TABLE promotions ADD COLUMN store_id    BIGINT;
ALTER TABLE promotions ADD COLUMN coupon_code VARCHAR(30);

ALTER TABLE promotions ADD CONSTRAINT promotions_bogo_check
    CHECK (type <> 'BUY_X_GET_Y' OR (buy_qty IS NOT NULL AND get_qty IS NOT NULL AND buy_qty >= 1 AND get_qty >= 1));
CREATE UNIQUE INDEX uq_promotions_coupon ON promotions (upper(coupon_code)) WHERE coupon_code IS NOT NULL;
