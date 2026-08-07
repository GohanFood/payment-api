# SDD — PaymentAPI v1.0.0

## 1. Visão Geral

O **PaymentAPI** é o microsserviço responsável pelo gerenciamento de pagamentos do sistema **deliveryAPI**. Ele gerencia o ciclo de vida completo de um pagamento: criação, processamento via Mercado Pago (PIX ou Cartão), consulta, reembolso e sincronização de status via webhook.

### Stack Tecnológica

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.4 |
| Build | Maven (mvnw) |
| Banco de Dados | PostgreSQL 16 |
| Migrations | Flyway |
| Mensageria | Apache Kafka (Spring Kafka 3.3) |
| Autenticação | JWT HS256 (jjwt 0.12.6) |
| Gateway Pagamento | Mercado Pago SDK Java 2.8.0 |
| Rate Limiting | Bucket4j 8.10 |
| Documentação | OpenAPI / Swagger (springdoc 2.8) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Containerização | Docker + Docker Compose |

---

## 2. Arquitetura

O PaymentAPI segue o padrão **Clean Architecture** (Hexagonal), com as seguintes camadas:

```
adapter/in/web          ← Controllers REST, filtros JWT
application/            ← Casos de uso, DTOs, serviços (inclui PaymentStatusSyncService)
domain/                 ← Entidades, value objects, exceções
  ├── payment/          ← Payment, PaymentStatus, exceções
  └── payment/gateway/  ← PaymentGatewayPort, PaymentGatewayRequest/Response
port/                   ← Interfaces (repositório, mensageria, gateway)
adapter/out/persistence ← JPA, Flyway, PostgreSQL
adapter/out/messaging   ← Kafka Producer / Consumer
adapter/out/gateway/    ← MercadoPagoGateway (PIX + Cartão + Reembolso + Consulta)
config/                 ← Security, Kafka, OpenAPI, MercadoPagoProperties
advice/                 ← Global Exception Handler (inclui GatewayUnavailableException 502)
```

### Fluxo de Pagamento via Cartão (Mercado Pago)

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│   Frontend   │      │   Payment API    │      │ Mercado Pago │
│ (MP.js       │      │   (Backend)      │      │   (Gateway)  │
│  CardForm)   │      │                  │      │              │
└──────┬───────┘      └────────┬─────────┘      └──────┬───────┘
       │                       │                       │
       │ 1. Inicializa         │                       │
       │    mp.cardForm()      │                       │
       │                       │                       │
       │ 2. Usuário preenche   │                       │
       │    dados do cartão    │                       │
       │                       │                       │
       │ 3. CardForm gera      │                       │
       │    CardToken          │                       │
       │                       │                       │
       │ 4. POST /payments/{id}/process             │
       │    { gatewayToken,    │                       │
       │      payerEmail,      │                       │
       │      installments,    │                       │
       │      paymentMethodId, │                       │
       │      issuerId, ... }  │                       │
       │──────────────────────>│                       │
       │                       │ 5. POST /v1/payments  │
       │                       │──────────────────────>│
       │                       │                       │
       │                       │ 6. { id, status }     │
       │                       │<──────────────────────│
       │                       │                       │
       │                       │ 7. approved → COMPLETED│
       │                       │    rejected → FAILED   │
       │                       │    pending  → PENDING  │
       │                       │                       │
       │                       │ 8. Kafka: payment.completed│
       │                       │──────────────────────>│
       │                       │                       │
       │ 9. Response           │                       │
       │<──────────────────────│                       │
       │                       │                       │
       │                       │ 10. Webhook IPN       │
       │                       │<──────────────────────│
       │                       │     (async, status)   │
```

### Fluxo de Pagamento via PIX (Mercado Pago)

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│   Frontend   │      │   Payment API    │      │ Mercado Pago │
└──────┬───────┘      └────────┬─────────┘      └──────┬───────┘
       │ 1. POST /payments     │                       │
       │    { paymentMethod:   │                       │
       │      "PIX",           │                       │
       │      payerEmail, ...} │                       │
       │──────────────────────>│                       │
       │                       │ 2. POST /v1/payments  │
       │                       │    (paymentMethodId:  │
       │                       │     "pix")            │
       │                       │──────────────────────>│
       │                       │                       │
       │                       │ 3. { id, status,      │
       │                       │      point_of_interaction:
       │                       │        qr_code,       │
       │                       │        qr_code_base64,│
       │                       │        ticket_url }   │
       │                       │<──────────────────────│
       │                       │                       │
       │ 4. Response c/ QR Code│                       │
       │<──────────────────────│                       │
       │                       │                       │
       │ 5. Exibe QR Code      │                       │
       │    para pagamento     │                       │
       │                       │                       │
       │                       │ 6. Webhook IPN        │
       │                       │<──────────────────────│
       │                       │     (approved/rejected)│
       │                       │                       │
       │                       │ 7. Kafka: payment.completed│
       │                       │    ou payment.failed  │
```

### Fluxo de Reembolso

```
1. Frontend → POST /api/v1/payments/refund
2. PaymentAPI → Mercado Pago: refund(paymentId)
3. PaymentAPI → status = REFUNDED
4. PaymentAPI → Kafka: payment.failed
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
| `paymentMethod` | String | Método: CREDIT_CARD, DEBIT_CARD, PIX, BOLETO |
| `status` | PaymentStatus | Estado atual do pagamento |
| `gatewayTransactionId` | String | ID da transação no Mercado Pago |
| `mpPaymentId` | Long | ID numérico no Mercado Pago (PIX) |
| `qrCode` | String | QR Code PIX copia-e-cola |
| `qrCodeBase64` | String | QR Code PIX em base64 |
| `ticketUrl` | String | URL do comprovante PIX |
| `payerEmail` | String | Email do comprador |
| `payerDocumentType` | String | Tipo de documento (CPF, CNPJ) |
| `payerDocumentNumber` | String | Número do documento |
| `expiresAt` | LocalDateTime | Data de expiração (PIX) |
| `createdAt` | LocalDateTime | Data de criação |
| `updatedAt` | LocalDateTime | Data da última atualização |

### PaymentStatus

- `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`
- `COMPLETED` → `REFUNDED`
- `PENDING` → `CANCELLED`

### Gateway Value Objects (domain/payment/gateway/)

| Classe | Descrição |
|--------|-----------|
| `PaymentGatewayPort` | Interface: `processCardPayment`, `refundPayment`, `getPayment` |
| `PaymentGatewayRequest` | Dados do cartão: cardToken, amount, installments, paymentMethodId, issuerId, payer |
| `PaymentGatewayResponse` | Resposta: externalId, externalStatus, externalStatusDetail, paymentTypeId |

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
    mp_payment_id BIGINT,
    qr_code TEXT,
    qr_code_base64 TEXT,
    ticket_url TEXT,
    payer_email VARCHAR(255),
    payer_document_type VARCHAR(10),
    payer_document_number VARCHAR(20),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_tx ON payments(gateway_transaction_id);
```

---

## 5. Endpoints

| Método | Path | Descrição | Auth |
|--------|------|-----------|------|
| `POST` | `/api/v1/payments` | Criar pagamento pendente (PIX gera QR Code) | ✅ |
| `POST` | `/api/v1/payments/{id}/process` | Processar pagamento via cartão (Mercado Pago) | ✅ |
| `GET` | `/api/v1/payments/{id}` | Buscar pagamento por ID | ✅ |
| `GET` | `/api/v1/payments?userId=` | Listar pagamentos do usuário | ✅ |
| `POST` | `/api/v1/payments/refund` | Reembolsar pagamento | ✅ |
| `POST` | `/api/v1/payments/webhook` | Webhook IPN do Mercado Pago | — |

---

## 6. Kafka Events

### Producer (PaymentAPI emite)

| Tópico | Quando |
|--------|--------|
| `payment.created` | Pagamento PENDING criado |
| `payment.completed` | Pagamento aprovado (COMPLETED) |
| `payment.failed` | Pagamento reembolsado (REFUNDED) ou rejeitado |

### Consumer (PaymentAPI escuta)

| Tópico | Ação | Status |
|--------|------|--------|
| `order.created` | Cria pagamento PENDING automaticamente | ⚠️ Apenas log — a implementar |
| `order.cancelled` | Reembolsa pagamento automaticamente | ⚠️ Apenas log — a implementar |

---

## 7. Integração Mercado Pago

### Configuração

| Variável | Descrição |
|----------|-----------|
| `MERCADOPAGO_ACCESS_TOKEN` | Access token (sandbox ou produção) |
| `MERCADOPAGO_PUBLIC_KEY` | Public key (usada no frontend MP.js) |

O SDK é auto-configurado no `@PostConstruct` do `MercadoPagoGateway`.

### Modo simulado

Quando `MERCADOPAGO_ACCESS_TOKEN` não está configurado (ex: ambiente de testes), o gateway opera em modo simulado:
- Cartão: retorna `approved` com ID fake
- PIX: retorna QR Code simulado
- Reembolso: retorna `refunded`
- Consulta: retorna `approved`

### Idempotência

Todas as chamadas ao Mercado Pago usam `X-Idempotency-Key` com o UUID do pagamento local (`payment.getId()`), garantindo que requisições duplicadas não criem pagamentos duplicados.

### Webhook IPN

Endpoint: `POST /api/v1/payments/webhook` (público, sem auth).
O `PaymentStatusSyncService` processa o webhook:
1. Extrai `data.id` do payload
2. Busca pagamento local por `gatewayTransactionId`
3. Consulta status atual no Mercado Pago via `GET /v1/payments/{id}`
4. Atualiza status local (COMPLETED / FAILED)
5. Emite evento Kafka correspondente

---

## 8. Segurança

- **Autenticação:** JWT Bearer token (compartilhado com user-api)
- **Autorização:** `@PreAuthorize("isAuthenticated()")` em todos os endpoints
- **Exceções:** `/actuator/health/**`, `/swagger-ui/**`, `/v3/api-docs/**` e `/api/v1/payments/webhook`
- **Rate Limiting:** Bucket4j via `RateLimitFilter` (a implementar)
- **JWT Secret:** `JWT_SECRET` via variável de ambiente

---

## 9. Deploy

### Docker Compose (isolado)
```bash
cd payment-api
export MERCADOPAGO_ACCESS_TOKEN="TEST-..."
export MERCADOPAGO_PUBLIC_KEY="TEST-..."
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

## 10. Testes

- **70 testes** cobrindo:
  - Domain (Payment, PaymentStatus)
  - Services (Create, Process, Get, List, Refund)
  - Controller (7 cenários REST)
  - Repository JPA (7 cenários)
  - Entity Mapper (5 cenários)
  - Kafka Producer (4 cenários)
  - Integração (8 cenários E2E — usa modo simulado do Mercado Pago)
- **JaCoCo**: mínimo 85% line coverage, 60% branch coverage
