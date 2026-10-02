# Backend Orders Challenge

This repository contains an Orders Service and a Payment Service backed by
PostgreSQL. The payment outcome is simulated and the Payment Service is called by
the Orders Service over HTTP.

## Run locally

Requirements: Java 21, Maven, and Docker.

1. Start PostgreSQL from the repository root:

   ```sh
   docker compose up -d
   ```

2. Start the Orders Service. It initializes its schema and inserts the sample
   products from `orders-service/src/main/resources/data.sql`:

   ```sh
   mvn -f orders-service/pom.xml \
     -Dspring-boot.run.workingDirectory="$(pwd)" spring-boot:run
   ```

3. In another terminal, start the Payment Service:

   ```sh
   mvn -f payment-service/pom.xml \
     -Dspring-boot.run.workingDirectory="$(pwd)" spring-boot:run
   ```

Run these commands from the repository root. The explicit working directory
ensures Spring Boot can discover the root-level `compose.yaml`, even though
Maven is running a service module's POM. IntelliJ's run configuration should
use the repository root as its working directory as well.

The Orders API listens on port `8080`; the Payment API listens on `8081`.
Both services use the local PostgreSQL database configured in their
`application.properties`.

## Run tests

Run tests from each service directory:

```sh
mvn test
```

The integration/context tests use PostgreSQL Testcontainers and require Docker.
See [docs/testing.md](docs/testing.md) for coverage and test setup.

## Documentation

- [API definitions](docs/api-definitions.md)
- [Architecture](docs/architecture.md)
- [Data model](docs/data-model.md)
- [Assumptions and scope](docs/assumptions.md)
- [Testing strategy](docs/testing.md)

### Additional Requirement  -  high-level design evolving the current solution to a larger scale

- [Evolution Design](docs/evolution-design.md)