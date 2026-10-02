## API Definitions

### Orders Service

#### `POST /orders`
Create a new order.

**Request body**
```json
{
  "customer_id": 123,
  "order_items": [
    { "product_id": 45, "quantity": 2}
  ]
}
```

**Response `201 Created`**
```json
{
  "order_id": 987,
  "status": "PENDING",
  "total_amount": 19.98,
  "order_items": [
    {
      "order_item_id": 12,
      "product_id": 45,
      "quantity": 2,
      "unit_price": 9.99
    }
  ]
}
```

**Response `400 Bad Request`** for an invalid request, including an empty
`order_items` list or a quantity less than one.

**Response `404 Not Found`** if any requested product does not exist.

---

#### `GET /orders/{order_id}`
Fetch order details.

**Response `200 OK`**
```json
{
  "order_id": 987,
  "customer_id": 123,
  "status": "PENDING",
  "total_amount": 39.98,
  "order_items": [
    { "order_item_id": 12, "product_id": 45, "quantity": 2, "unit_price": 19.99 }
  ]
}
```

**Response `404 Not Found`** if the order does not exist.

---

#### `POST /orders/{order_id}/pay`
Trigger payment for an order. Orchestrates a call to the Payment Service.

**Request body:** _(None. Order details are looked up server side.)_

**Response `200 OK`**
```json
{
  "order_id": 987,
  "status": "APPROVED"
}
```

**Response `409 Conflict`** (Payment DECLINED)
```json
{
  "order_id": 987,
  "status": "DECLINED",
  "reason": "insufficient funds"
}
```

**Response `409 Conflict`** (Order not in `PENDING` state. Prevents double payment)

**Response `404 Not Found`** if the order does not exist.

**Response `503 Service Unavailable`** if the Payment Service returns an
upstream server error or cannot be reached before the configured timeout.

---

### Payment Service

#### `POST /payments`
Process a payment for a given order. Called internally by the Orders Service. Not exposed to the outside directly.

**Security:** Internal-only. Service authentication/authorization is out of
scope for this exercise.

**Request body**
```json
{
  "order_id": 987,
  "amount": 39.98
}
```

Requests with a missing order ID, missing amount, or an amount below `0.01` are
rejected with `400 Bad Request`.

**Response `201 Created`** (approved)
```json
{
  "order_id": 987,
  "status": "APPROVED"
}
```

**Response `201 Created`** (declined; a business outcome, not a failed request)
```json
{
  "order_id": 987,
  "status": "DECLINED",
  "reason": "insufficient funds"
}
```

**Response `503 Service Unavailable`** if the simulated payment provider is
unavailable. This distinguishes infrastructure failure from a legitimate decline.

---
