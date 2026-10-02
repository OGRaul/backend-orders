## Data Model

The services currently use one PostgreSQL database. Status columns are PostgreSQL
enum types, not `varchar` columns. Monetary values use `DECIMAL(10,2)`.

### products

| Column | Type |
|----------|----------|
| id | `BIGSERIAL` primary key |
| name | `VARCHAR(255) NOT NULL` |
| price | `DECIMAL(10,2) NOT NULL` |

### orders

| Column | Type |
|----------|----------|
| id | `BIGSERIAL` primary key |
| customer_id | `BIGINT NOT NULL` |
| status | `order_status NOT NULL` (`PENDING`, `PAID`, `FULFILLED`, `CANCELLED`) |
| total_amount | `DECIMAL(10,2) NOT NULL` |
| created_at | `TIMESTAMP WITH TIME ZONE NOT NULL`, defaults to `now()` |

### order_items

| Column | Type |
|----------|----------|
| id | `BIGSERIAL` primary key |
| order_id | `BIGINT NOT NULL` |
| product_id | `BIGINT NOT NULL`, foreign key to `products.id` |
| quantity | `INTEGER NOT NULL`, must be greater than zero |
| unit_price | `DECIMAL(10,2) NOT NULL` |

### payments

| Column | Type |
|----------|----------|
| id | `BIGSERIAL` primary key |
| order_id | `BIGINT NOT NULL` |
| amount | `DECIMAL(10,2) NOT NULL` |
| status | `payment_status NOT NULL` (`APPROVED`, `DECLINED`) |
| reason | `VARCHAR(255)`, required for a declined payment |
| created_at | `TIMESTAMP WITH TIME ZONE NOT NULL`, defaults to `now()` |

`payments.order_id` intentionally has no database foreign key to `orders`: the
payment service treats the order ID as an external reference. The current schema
also does not declare a foreign key from `order_items.order_id` to `orders.id`.

Indexes are defined for `orders.customer_id`, `order_items.order_id`,
`order_items.product_id`, and `payments.order_id`.