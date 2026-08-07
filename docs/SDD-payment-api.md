# SDD — PaymentAPI v1.0.0

## 1. Visão Geral

O **PaymentAPI** é o microsserviço responsável pelo gerenciamento de pagamentos do sistema **deliveryAPI**. Ele gerencia o ciclo de vida completo de um pagamento: criação, processamento via gateway, consulta e reembolso.

### Stack Tecnológica

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.4 |
| Build | Maven (mvnw) |
| Banco de Dados | PostgreSQL 16 |
| Migrations | Flyway |
| Mensageria | Apache Kafka (Spring Kafka 3.3) |
| Autenticação | JWT (jjwt 0.12.6) |
| Rate Limiting | Bucket4j 8.10 |
| Documentação | OpenAPI / Swagger (springdoc 2.8) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Containerização | Docker + Docker Compose |

---

## 2. Arquitetura

O PaymentAPI segue o padrão **Clean Architecture** (Hexagonal), com as seguintes camadas:

```
adapter/in/web          ← Controllers REST, filtros JWT
application/            ← Casos de uso, DTOs, serviços
domain/                 ← Entidades, value objects, exceções
port/                   ← Interfaces (repositório, mensageria)
adapter/out/persistence ← JPA, Flyway, PostgreSQL
adapter/out/messaging   ← Kafka Producer / Consumer
config/                 ← Security, Kafka, OpenAPI
advice/                 ← Global Exception Handler
```

### Fluxo de Pagamento

```
1. Frontend/OrderAPI → POST /payments (cria pagamento PENDING)
2. PaymentAPI → Kafka: payment.created
3. Frontend → POST /payments/{id}/process (gatewayToken)
4. PaymentAPI → Gateway (simulado) → COMPLETED
5. PaymentAPI → Kafka: payment.completed
```

### Fluxo de Reembolso

```
1. Frontend → POST /payments/refund
2. PaymentAPI → REFUNDED
3. PaymentAPI → Kafka: payment.failed
```

---

## 3. Modelo de Domínio

### Payment

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | UUID | Identificador único |
| `userId` | UUID | ID do usuário dono do pagamento |
| `orderId` | UUID | ID do pedido associado |
| `amount` | BigDecimal | Valor do pagamento (> 0) |
| `paymentMethod` | String | Método de pagamento (CREDIT_CARD, PIX, etc.) |
| `status` | PaymentStatus | Estado atual do pagamento |
| `gatewayTransactionId` | String | ID da transação no gateway (nullable) |
| `createdAt` | LocalDateTime | Data de criação |
| `updatedAt` | LocalDateTime | Data da última atualização |

### PaymentStatus

- `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`
- `COMPLETED` → `REFUNDED`
- `PENDING` → `CANCELLED`

---

## 4. Banco de Dados

**Database:** `paymentdb`
**Schema:** Flyway-managed (`V1__create_payments_table.sql`)

```sql
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_transaction_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
```

---

## 5. Endpoints

| Método | Path | Descrição | Auth |
|--------|------|-----------|------|
| `POST` | `/api/v1/payments` | Criar pagamento pendente | ✅ |
| `POST` | `/api/v1/payments/{id}/process` | Processar pagamento | ✅ |
| `GET` | `/api/v1/payments/{id}` | Buscar pagamento por ID | ✅ |
| `GET` | `/api/v1/payments?userId=` | Listar pagamentos do usuário | ✅ |
| `POST` | `/api/v1/payments/refund` | Reembolsar pagamento | ✅ |

---

## 6. Kafka Events

### Producer (PaymentAPI emite)

| Tópico | Quando |
|--------|--------|
| `payment.created` | Pagamento PENDING criado |
| `payment.completed` | Pagamento aprovado (COMPLETED) |
| `payment.failed` | Pagamento reembolsado (REFUNDED) |

### Consumer (PaymentAPI escuta)

| Tópico | Ação |
|--------|------|
| `order.created` | Cria pagamento PENDING automaticamente |
| `order.cancelled` | Reembolsa pagamento automaticamente |

---

## 7. Segurança

- **Autenticação:** JWT Bearer token (compartilhado com user-api)
- **Autorização:** `@PreAuthorize("isAuthenticated()")` em todos os endpoints
- **Rate Limiting:** Bucket4j via `RateLimitFilter` (a implementar)
- **JWT Secret:** `JWT_SECRET` via variável de ambiente

---

## 8. Deploy

### Docker Compose (isolado)
```bash
cd payment-api
docker compose up -d
```

### Docker Compose (projeto completo)
```bash
cd deliveryAPI
docker compose up -d
```

### Portas

| Serviço | Porta |
|---------|-------|
| payment-api | 8082 |
| postgres-payment | 5434 |
| Kafka | 9092 |
| Zookeeper | 2181 |

---

## 9. Testes

- **70 testes** cobrindo:
  - Domain (Payment, PaymentStatus)
  - Services (Create, Process, Get, List, Refund)
  - Controller (7 cenários REST)
  - Repository JPA (7 cenários)
  - Entity Mapper (5 cenários)
  - Kafka Producer (4 cenários)
  - Integração (8 cenários E2E)
- **JaCoCo**: mínimo 85% line coverage, 60% branch coverage
