DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'payment_status'
    ) THEN
CREATE TYPE payment_status AS ENUM (
            'APPROVED',
            'DECLINED'
        );
END IF;
END
$$;
@@

CREATE TABLE IF NOT EXISTS payments (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          status payment_status NOT NULL,
                          reason VARCHAR(255),
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                          CONSTRAINT reason_required_on_decline CHECK (
                              status <> 'DECLINED' OR reason IS NOT NULL
                              )
);
@@
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
@@