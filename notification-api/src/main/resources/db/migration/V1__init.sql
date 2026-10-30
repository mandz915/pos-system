CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    event_id   VARCHAR(64)  UNIQUE,
    type       VARCHAR(30)  NOT NULL,
    channel    VARCHAR(10)  NOT NULL DEFAULT 'EMAIL' CHECK (channel IN ('EMAIL', 'SMS')),
    recipient  VARCHAR(150) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    store_id   BIGINT,
    status     VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts   INT          NOT NULL DEFAULT 0,
    last_error VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ
);

CREATE INDEX idx_notifications_pending ON notifications (id) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_store ON notifications (store_id, id DESC);
