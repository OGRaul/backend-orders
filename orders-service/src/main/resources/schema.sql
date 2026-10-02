
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'order_status'
    ) THEN
CREATE TYPE order_status AS ENUM (
            'PENDING',
            'PAID',
            'FULFILLED',
            'CANCELLED'
        );
END IF;
END
$$;
@@

CREATE TABLE IF NOT EXISTS products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(10,2) NOT NULL
);
@@

CREATE TABLE IF NOT EXISTS orders (
                        id BIGSERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        status order_status NOT NULL,
                        total_amount DECIMAL(10,2) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
@@

CREATE TABLE IF NOT EXISTS order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL REFERENCES products(id),
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price DECIMAL(10,2) NOT NULL
);
@@
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
@@
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
@@
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
@@