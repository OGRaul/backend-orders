# Assumptions

### Requirements assumptions
- The product catalog is predefined.
- Product prices are snapshotted when an order is created.
- Inventory management is handled elsewhere.
- A single currency is used.
### Trade-offs
- Synchronous REST communication is used for simplicity.
- Orders and Payment are separate services, but currently share a PostgreSQL database.
- Payment processing is simulated.
### Out of Scope
- Authentication and authorization.
- Inventory and shipment tracking.
- Real payment-provider integration.
- Kafka and ERP integration.
