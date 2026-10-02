#Testing Strategy

## Goals

The testing strategy focuses on validating:

- API contracts and HTTP responses
- Order business rules
- Payment state transitions
- Error handling
- Persistence and service integration

## Test Levels

### Unit tests

Service-layer tests validate business logic in isolation, with repositories and
the inter-service client mocked.

Current coverage:
- **Orders:** create a pending order, calculate its total and save its items; throw
  when a requested product is missing; return order details or throw when the
  order is missing.
- **Order payments:** mark an order PAID after approval; leave it PENDING after
  decline; reject missing or non-PENDING orders; propagate payment-provider
  unavailability without saving the order.
- **Payment service:** save approved and declined payments; throw
  `PaymentUnavailableException` and do not save when the provider is unavailable.

These tests do not exercise database persistence or real HTTP calls between services.

### Controller/API tests

Controller tests validate the external HTTP contract, including:

- request validation
- HTTP status codes
- response payloads
- exception-to-HTTP mapping

### Integration tests

Orders integration tests run the Spring application against a temporary
PostgreSQL Testcontainer. They cover order creation, request validation,
retrieval, and payment orchestration. The payment service is represented by
WireMock in payment-flow tests; these tests do not start the real payment
service. Order-creation tests load product fixtures from
`src/test/resources/sql/order-test-data.sql`. Tests verify order creation
rollback when a product is missing, and check that retrieval returns persisted
order items.

The PostgreSQL container is shared across the integration-test classes so it
remains available while Spring reuses its cached application context. The
container is isolated from the development database configured for normal app
runs.

## Key Scenarios

### Order creation

| Scenario | Expected result |
|---|---|
| Valid order with seeded products | 201 Created; PENDING status, calculated total, and order items |
| Invalid request | 400 Bad Request |
| Unknown product | 404 Not Found |

### Order retrieval

| Scenario | Expected result |
|---|---|
| Existing order | 200 OK |
| Unknown order | 404 Not Found |

### Payment

| Scenario | Expected result |
|---|---|
| Approved WireMock payment response | 200 OK; order persisted as PAID |
| Declined WireMock payment response | 409 Conflict; order remains PENDING |
| Non-PENDING order | 409 Conflict |
| Payment service returns an upstream 5xx | 503 Service Unavailable |

The payment-service unit and controller tests separately cover approved and
declined outcomes, request validation, and `PaymentUnavailableException`.
Payment-client tests also cover upstream 5xx responses and transport failures.

## What is not currently tested

The following are intentionally outside the scope of the take-home:

- performance/load testing
- real payment-provider integration
- running both microservices together against PostgreSQL
- Kafka/event publishing
- Kubernetes deployment
- authentication/authorization
- real external infrastructure failure testing beyond the simulated payment-provider outage

## Manual API Testing

Postman was used during development to manually exercise the complete
order flow across both services.

The automated test suite remains the primary repeatable validation mechanism.