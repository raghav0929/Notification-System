# Notification System

A multi-service, event-driven notification platform built with Spring Boot and Apache Kafka. Simulates the core pipeline of a production-grade notification system — ingestion, preference-aware routing, and channel delivery — with a strong focus on **reliability under failure**, not just the happy path.

This started from a high-level design covering a much larger scope (multi-channel delivery, bulk segmentation, fraud/rule engines). This implementation is a deliberately scoped-down slice: a complete, working, Email-only pipeline that demonstrates the same architectural patterns a full system would need, built incrementally with real debugging along the way.

---

## Architecture

```
Client
  │
  ▼
┌─────────────────────┐      ┌───────────────────────┐      ┌───────────────────────┐
│ Notification Service │      │  Dispatcher Service    │      │  Email Handler Service │
│  (port 8081)         │      │  (port 8082)           │      │  (port 8083)           │
│                       │      │                         │      │                         │
│ • Validates request   │      │ • Checks preferences    │      │ • Sends email (mocked) │
│ • Idempotency check    │─Kafka→ • Checks quiet hours     │─Kafka→ • Tracks delivery      │
│ • Priority resolution   │      │ • Calls Rate Limiter    │      │ • Retries on failure    │
│ • Outbox pattern         │      │ • Routes to channel      │      │                         │
└─────────────────────┘      └───────────────────────┘      └───────────────────────┘
        │                              │                              │
        ▼                              ▼                              ▼
  Postgres:                      Postgres:                      Postgres:
  notification_service            dispatcher_service              email_handler_service
  (idempotency_keys,               (user_preferences)              (notification_tracker)
   outbox_events)

Kafka topics: notifications.validated → notifications.email
```

Each service owns its own database (database-per-service) and communicates exclusively through Kafka — no service calls another service's REST API directly, except the Dispatcher's call to an external Rate Limiter service.

---

## Services

### 1. Notification Service (`notification-service`)
Entry point for all clients.

- `POST /api/v1/notifications` — accepts a notification request, validates it, and hands it off downstream
- Deduplicates requests using a client-supplied `idempotencyKey` — a retried request with the same key returns `409 Conflict` instead of creating a duplicate notification
- Auto-resolves priority from notification `type` if not explicitly provided (e.g. OTP → HIGH)

### 2. Dispatcher Service (`dispatcher-service`)
The decision layer — determines whether a notification should actually be sent.

- Consumes `notifications.validated`
- Checks `user_preferences` (opted in/out per channel, quiet hours — including ranges that span midnight)
- Calls an external Rate Limiter service before allowing a send
- Publishes to a channel-specific topic (`notifications.email`) if all checks pass
- **Defaults to allowed** if no preference row exists (opt-out model, not opt-in)
- **Fails open** if the Rate Limiter is unreachable — availability prioritized over strict enforcement

### 3. Email Handler Service (`email-handler-service`)
Terminal service — sends the notification and tracks the outcome.

- Consumes `notifications.email`
- Sends via a mock email sender (swappable for a real vendor like SendGrid/SES)
- Writes delivery status to `notification_tracker` (`SENT`, `RETRYING`, `FAILED`)
- Retries up to 3 times via Kafka redelivery before giving up

---

## The core engineering problem this project solves: the Dual-Write Problem

Early in development, notifications were saved to Postgres and published to Kafka as two independent operations. When Kafka was unavailable, the database write still succeeded — leaving orphaned records with no corresponding event ever published. This is a well-known distributed systems failure mode: **writing to two systems that don't share a transaction can never be made atomic by ordering alone.**

### The fix: Transactional Outbox Pattern

Instead of publishing to Kafka directly from the request thread:

1. The idempotency record **and** a serialized copy of the Kafka message are written to an `outbox_events` table, inside the **same database transaction**. Either both succeed or both roll back.
2. A background poller (`@Scheduled`, every 3 seconds) reads `PENDING` outbox rows and publishes them to Kafka, marking them `PUBLISHED` on success.
3. If Kafka is down, rows simply stay `PENDING` — nothing is lost. When Kafka recovers, the next poll cycle publishes them automatically.

**Verified by testing**: killing the Kafka broker mid-flow, confirming requests still succeed and persist correctly, then restarting Kafka and watching the backlog drain automatically with zero data loss.

### The same problem, consumer-side

The Dispatcher and Email Handler services have their own version of this risk: Spring Kafka's default auto-commit can advance a consumer's offset *before* confirming the downstream action (a republish, an email send) actually succeeded. Both services disable auto-commit and use **manual acknowledgment** — the offset only commits after the action succeeds, so a failure triggers Kafka redelivery instead of silent message loss.

---

## Key design decisions

| Decision | Reasoning |
|---|---|
| Postgres instead of Cassandra for preferences/tracking | Simpler to run locally; sufficient for this data volume. Cassandra would be the right call at much higher write throughput — a tradeoff worth naming rather than hiding. |
| Database-per-service | Each service owns its schema; no cross-service DB access. Real microservices boundary, not just folder separation. |
| Opt-out (not opt-in) preference default | If no preference record exists, notifications are allowed. A deliberate product decision, not an oversight. |
| Fail-open rate limiting | A Rate Limiter outage shouldn't block all notifications system-wide. Availability chosen over strict enforcement — a defensible but debatable tradeoff. |
| Manual Kafka offset commits | Prevents silent message loss on downstream failures, at the cost of potential reprocessing (at-least-once, not exactly-once, delivery). |
| Transactional Outbox over direct publish | Guarantees no orphaned DB state if Kafka is temporarily unavailable. |

---

## Known limitations (intentional scope boundaries, not oversights)

- No dead-letter queue — messages that exhaust retries are marked `FAILED` but not routed anywhere for manual inspection
- No retry cap on the outbox poller — a permanently malformed message would retry indefinitely
- Single channel (Email) — SMS and in-app push were part of the original design but out of scope for this build
- Rate Limiter is called via a defined contract but not included in this repo
- No authentication/authorization on the public API

---

## Tech Stack

- **Java 17**, **Spring Boot**
- **Apache Kafka** (KRaft mode, no Zookeeper)
- **PostgreSQL** (one instance per service)
- **Spring Data JPA**, **Spring Kafka**
- **Lombok**

---

## Running locally

### Prerequisites
- Java 17+
- Apache Kafka (KRaft mode)
- PostgreSQL
- Maven

### 1. Start Kafka
```bash
bin/kafka-storage.sh format -t <cluster-uuid> --standalone -c config/server.properties
bin/kafka-server-start.sh config/server.properties
```

Create the required topics:
```bash
bin/kafka-topics.sh --create --topic notifications.validated --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
bin/kafka-topics.sh --create --topic notifications.email --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### 2. Set up databases
Create three Postgres databases: `notification_service`, `dispatcher_service`, `email_handler_service`. Run the DDL in each service's `/docs/schema.sql` (or see the SQL in each service's README section).

### 3. Update credentials
Update `application.yml` in each service with your local Postgres username/password.

### 4. Start the services
```bash
cd notification-service && mvn spring-boot:run
cd dispatcher-service && mvn spring-boot:run
cd email-handler-service && mvn spring-boot:run
```

### 5. Send a test request
```bash
curl -X POST http://localhost:8081/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "u123",
    "type": "TRANSACTION_ALERT",
    "channel": "EMAIL",
    "idempotencyKey": "test-001",
    "payload": {"subject": "Payment received", "body": "Your payment of Rs.500 was successful"}
  }'
```

Watch the logs across all three services — the request should flow through validation, preference/rate-limit checks, and mock delivery, landing in `email_handler_service.notification_tracker` with status `SENT`.

---

## Roadmap

- [ ] Docker Compose for one-command local setup
- [ ] Dead-letter queue for permanently failed messages
- [ ] Real email vendor integration (SendGrid/SES)
- [ ] Additional channels: SMS, in-app push
- [ ] Bulk notification + user segmentation (from the original design)
