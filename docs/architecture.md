# Order API — Architecture

## 1. Domain Model

### product

- `id`
- `name`
- `price`

### order

- `id`
- `customer_id`
- `order_items`
- `status`
- `created_at`

### order_items

- `id`
- `order_id`
- `product_id`
- `quantity`
- `unit_price`

### payment

- `id`
- `order_id`
- `amount`
- `status`
- `reason`
- `created_at`

An order is placed by a customer and contains one or more order items. Each order item references
a product and stores the product price at the time the order is created and the quantity ordered.

## 2. Order Lifecycle

The implemented flow currently creates orders as `PENDING` and changes them to
`PAID` only after payment approval. `FULFILLED` and `CANCELLED` are modeled
statuses but do not currently have API operations.

```text
PENDING
   |
   v
 PAID
   |
   v
FULFILLED

PENDING --> CANCELLED
```
## 3. Payment status

```text
APPROVED / DECLINED
```

A declined payment does not change the order state. The order remains `PENDING`
and can be retried. A provider outage is returned as a service-unavailable error.

## 4. Domain Boundaries
```text
Orders Service
  owns:
    order
    order_item
    product
    Order lifecycle

Payment Service
  owns:
    payment
    Payment status
    simulated payment-provider interaction
```

In such a simple and small in scope solution two fully separate services is likely unnecessary
and not a good cost/benefit design choice. However since we are assuming this would be just a part
of a much larger real system I chose to implement at least 2 microservices in order to be more realistic
as well as to set the stage for how such a simple system would have to change to scale up to 500 thousand
daily orders with spikes.

## 5. Data Model

Database tables:

- product
- order
- order_item
- payment

For simplicity, this implementation uses a single PostgreSQL database.

The services currently share this database for the challenge. In a production
architecture, the Payment Service would typically own a separate database to
reinforce service boundaries and enable independent scaling.

Each service initializes its schema from `schema.sql`; the Orders Service also
seeds the sample products from `data.sql`. This keeps setup lightweight for the
challenge. In production, a migration tool such as Flyway would be preferable
for managing versioned schema changes safely.


The tables are defined in docs/data-model.md


## 6. Security

For this challenge most of the security questions are out of scope however it's
important to clarify that an api-gateway as a first line security
to do simple general validations.

The orders service would need to have an authentication system like oauth (for example 
using a bearer token to confirm the user sending the request to create an order is
who he says he is) and for authorization (for example checking if the user making a 
request for anything regarding an order has permission to do anything with that order).


The payment Service is treated as an internal service and is not 
to be exposed to the outside, only to be called from our orders service. However 
in a real environment, it would still require authenticated service-to-service 
communication. It could for example use the OAuth client credentials + a 
JSON Web Signature (JWS) to create a signed data-integrity token instead of sending
the amount and order id in plain text and trusting it blindly. Kafka could also come in
handy here using an outbox pattern to shift the trust to "who can publish to this topic"
but I will get to that in the high-level design to improve this simple api implementation.









