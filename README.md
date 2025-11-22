# Payment Service

A comprehensive, production-ready Reactive Payment Service built with Spring Boot WebFlux, R2DBC (PostgreSQL), Redis, and Kafka.

## Features

- **Payment Processing**: Supports Razorpay, Stripe, PayTM, and Cashfree.
- **Idempotency**: Ensures safe retries using `Idempotency-Key` header (Redis + DB).
- **Payouts**: Commission-based payouts to labour accounts (10% commission deduction).
- **Event Driven**: Publishes payment and notification events to Kafka.
- **Security**: JWT-based authentication and validation.
- **Reactive**: Fully non-blocking stack.

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

## Getting Started

### 1. Start Infrastructure

Run the following command to start PostgreSQL, Redis, Kafka, and Zookeeper:

\`\`\`bash
docker-compose up -d
\`\`\`

This will also initialize the database schema from `src/main/resources/schema.sql`.

### 2. Build and Run

\`\`\`bash
mvn clean install
mvn spring-boot:run
\`\`\`

The application will start on port `8080`.

## API Endpoints

### Payments

- **Initiate Payment**
  - `POST /api/payments/initiate`
  - Headers: `Authorization: Bearer <token>`, `Idempotency-Key: <uuid>`
  - Body:
    \`\`\`json
    {
      "userId": "user-123",
      "jobId": "job-456",
      "amount": 100.00,
      "currency": "USD",
      "provider": "stripe"
    }
    \`\`\`

- **Verify Payment (Webhook/Manual)**
  - `POST /api/payments/verify`
  - Body:
    \`\`\`json
    {
      "paymentId": "payment-uuid",
      "providerTransactionId": "tx_123",
      "status": "SUCCESS",
      "signature": "optional_sig"
    }
    \`\`\`

- **Get Status**
  - `GET /api/payments/{paymentId}`

### Payouts

- **Process Payout**
  - `POST /api/payouts/process`
  - Body:
    \`\`\`json
    {
      "labourId": "labour-789",
      "jobId": "job-456",
      "totalAmount": 100.00
    }
    \`\`\`
  - *Note: Service automatically calculates 10% commission. Payout amount will be 90.00.*

## Configuration

- **Database**: `jdbc:postgresql://localhost:5432/payment_db` (via R2DBC)
- **Redis**: `localhost:6379`
- **Kafka**: `localhost:9092`

## Testing

Use the provided `src/test/java` structure to add integration tests.
