# API Contract — PaymentAPI v1

| Field | Value |
|-------|-------|
| **Base URL** | `http://payment-api.delivery.internal/api/v1` |
| **Format** | JSON (application/json) |

---

## POST /api/v1/payments
**Purpose:** Criar um novo pagamento pendente
**Auth:** Bearer JWT
**Rate Limit:** 30 req/min

**Request:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD"
}
```
> **Campos obrigatórios:** `userId`, `orderId`, `amount`, `paymentMethod`
> `amount` deve ser > 0
> `paymentMethod` valores esperados: `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `BOLETO`

**Response 201:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "PENDING",
  "gatewayTransactionId": null,
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:00:00Z"
}
```

**Kafka Event:** `payment.created` → `{ "paymentId": "...", "orderId": "..." }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | Campos obrigatórios ausentes ou amount ≤ 0 |
| 401 | UNAUTHORIZED | Token ausente ou inválido |

---

## POST /api/v1/payments/{id}/process
**Purpose:** Processar pagamento pendente via gateway de pagamento
**Auth:** Bearer JWT
**Rate Limit:** 20 req/min

**Path param:** `id` (UUID do pagamento)

**Request:**
```json
{
  "gatewayToken": "tok_abc123def456"
}
```
> **Campos obrigatórios:** `gatewayToken`

**Response 200:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "COMPLETED",
  "gatewayTransactionId": "GW-abc123def456",
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:05:00Z"
}
```

**Kafka Event:** `payment.completed` → `{ "paymentId": "...", "orderId": "..." }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | gatewayToken ausente |
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 404 | PAYMENT_NOT_FOUND | Pagamento não encontrado |
| 409 | PAYMENT_ALREADY_PROCESSED | Pagamento já foi processado |
| 502 | GATEWAY_UNAVAILABLE | Gateway de pagamento indisponível |

---

## GET /api/v1/payments/{id}
**Purpose:** Buscar pagamento por ID
**Auth:** Bearer JWT
**Rate Limit:** 100 req/min

**Path param:** `id` (UUID do pagamento)

**Response 200:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "COMPLETED",
  "gatewayTransactionId": "GW-abc123def456",
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:05:00Z"
}
```

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 404 | PAYMENT_NOT_FOUND | Pagamento não encontrado |

---

## GET /api/v1/payments
**Purpose:** Listar pagamentos de um usuário
**Auth:** Bearer JWT
**Rate Limit:** 100 req/min

**Query params:** `userId` (obrigatório, UUID)

**Response 200:**
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "orderId": "660e8400-e29b-41d4-a716-446655440002",
    "amount": 150.00,
    "paymentMethod": "CREDIT_CARD",
    "status": "PENDING",
    "gatewayTransactionId": null,
    "createdAt": "2026-08-07T15:00:00Z",
    "updatedAt": "2026-08-07T15:00:00Z"
  }
]
```
> Retorna array vazio `[]` se não houver pagamentos

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | userId ausente |
| 401 | UNAUTHORIZED | Token ausente ou inválido |

---

## POST /api/v1/payments/refund
**Purpose:** Reembolsar pagamento concluído
**Auth:** Bearer JWT
**Rate Limit:** 10 req/min

**Request:**
```json
{
  "paymentId": "770e8400-e29b-41d4-a716-446655440003"
}
```
> **Campos obrigatórios:** `paymentId`

**Response 200:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "REFUNDED",
  "gatewayTransactionId": "GW-abc123def456",
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:10:00Z"
}
```

**Kafka Event:** `payment.failed` → `{ "paymentId": "...", "orderId": "..." }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 404 | PAYMENT_NOT_FOUND | Pagamento não encontrado |
| 409 | PAYMENT_ALREADY_PROCESSED | Pagamento já reembolsado |

---

## Kafka Topics

### Producers (PaymentAPI → outros serviços)

| Tópico | Evento | Payload |
|--------|--------|---------|
| `payment.created` | Pagamento pendente criado | `{ "paymentId": "uuid", "orderId": "uuid" }` |
| `payment.completed` | Pagamento aprovado pelo gateway | `{ "paymentId": "uuid", "orderId": "uuid" }` |
| `payment.failed` | Pagamento reembolsado | `{ "paymentId": "uuid", "orderId": "uuid" }` |

### Consumers (outros serviços → PaymentAPI)

| Tópico | Evento | Ação |
|--------|--------|------|
| `order.created` | Novo pedido criado | Cria pagamento pendente automaticamente |
| `order.cancelled` | Pedido cancelado | Reembolsa pagamento automaticamente |

---

## PaymentStatus Enum

| Status | Descrição |
|--------|-----------|
| `PENDING` | Pagamento criado, aguardando processamento |
| `PROCESSING` | Gateway está processando (uso futuro) |
| `COMPLETED` | Pagamento aprovado pelo gateway |
| `FAILED` | Pagamento rejeitado pelo gateway |
| `REFUNDED` | Pagamento reembolsado |
| `CANCELLED` | Pagamento cancelado |

---

## Error Envelope
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Pagamento não encontrado: 770e8400-e29b-41d4-a716-446655440099",
  "timestamp": "2026-08-07T15:00:00"
}
```
