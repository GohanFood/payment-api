# API Contract — PaymentAPI v1

| Field | Value |
|-------|-------|
| **Base URL** | `http://payment-api.delivery.internal/api/v1` |
| **Format** | JSON (application/json) |
| **Gateway** | Mercado Pago (PIX + Cartão via CardForm) |

---

## POST /api/v1/payments
**Purpose:** Criar um novo pagamento pendente
**Auth:** Bearer JWT
**Rate Limit:** 30 req/min

**Request (Cartão / Boleto / Débito):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD"
}
```

**Request (PIX):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 89.90,
  "paymentMethod": "PIX",
  "payerEmail": "cliente@email.com",
  "payerFirstName": "João",
  "payerLastName": "Silva",
  "payerDocumentType": "CPF",
  "payerDocumentNumber": "19119119100"
}
```
> **Campos obrigatórios:** `userId`, `orderId`, `amount`, `paymentMethod`.
> `amount` deve ser > 0.
> `paymentMethod`: `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `BOLETO`.
> Para PIX: `payerEmail`, `payerDocumentType` e `payerDocumentNumber` são recomendados.

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
  "mpPaymentId": null,
  "qrCode": null,
  "qrCodeBase64": null,
  "ticketUrl": null,
  "payerEmail": null,
  "payerDocumentType": null,
  "payerDocumentNumber": null,
  "expiresAt": null,
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:00:00Z"
}
```
> Para PIX, se `MERCADOPAGO_ACCESS_TOKEN` estiver configurado, `qrCode`, `qrCodeBase64`, `ticketUrl` e `mpPaymentId` são preenchidos.

**Kafka Event:** `payment.created` → `{ "paymentId": "uuid", "orderId": "uuid" }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | Campos obrigatórios ausentes ou amount ≤ 0 |
| 401 | UNAUTHORIZED | Token ausente ou inválido |

---

## POST /api/v1/payments/{id}/process
**Purpose:** Processar pagamento pendente via Mercado Pago
**Auth:** Bearer JWT
**Rate Limit:** 20 req/min

**Path param:** `id` (UUID do pagamento)

**Request (obrigatório):**
```json
{
  "gatewayToken": "CARD_TOKEN_DO_MERCADOPAGO_JS",
  "payerEmail": "cliente@email.com",
  "installments": 1,
  "paymentMethodId": "visa",
  "issuerId": "0",
  "identificationType": "CPF",
  "identificationNumber": "19119119100",
  "description": "Pedido #123"
}
```
> **Campos obrigatórios:** `gatewayToken`, `payerEmail`.
> `gatewayToken`: CardToken gerado pelo MercadoPago.js CardForm no frontend.
> `installments` (default 1): número de parcelas.
> `paymentMethodId`: bandeira do cartão (`visa`, `master`, `elo`, `amex`, etc.).
> `issuerId`: ID do banco emissor.
> `identificationType/Number`: documento do comprador (recomendado para melhor aprovação).

**Response 200 (aprovado):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "COMPLETED",
  "gatewayTransactionId": "1234567890123456",
  "mpPaymentId": null,
  "qrCode": null,
  "qrCodeBase64": null,
  "ticketUrl": null,
  "payerEmail": "cliente@email.com",
  "payerDocumentType": null,
  "payerDocumentNumber": null,
  "expiresAt": null,
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:05:00Z"
}
```
> `gatewayTransactionId` contém o ID do pagamento no Mercado Pago.
> Status pode ser `COMPLETED` (aprovado), `FAILED` (rejeitado) ou `PENDING` (aguardando).

**Kafka Event (se aprovado):** `payment.completed` → `{ "paymentId": "uuid", "orderId": "uuid" }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | `gatewayToken` ou `payerEmail` ausentes |
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 404 | PAYMENT_NOT_FOUND | Pagamento não encontrado |
| 409 | PAYMENT_ALREADY_PROCESSED | Pagamento já foi processado |
| 502 | GATEWAY_UNAVAILABLE | Mercado Pago indisponível |

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
  "gatewayTransactionId": "1234567890123456",
  "mpPaymentId": 123456789,
  "qrCode": "00020126580014...",
  "qrCodeBase64": "iVBORw0KGgo...",
  "ticketUrl": "https://www.mercadopago.com.br/...",
  "payerEmail": "cliente@email.com",
  "payerDocumentType": "CPF",
  "payerDocumentNumber": "19119119100",
  "expiresAt": "2026-08-08T15:00:00Z",
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
    "status": "COMPLETED",
    "gatewayTransactionId": "1234567890123456",
    "mpPaymentId": null,
    "qrCode": null,
    "qrCodeBase64": null,
    "ticketUrl": null,
    "payerEmail": null,
    "payerDocumentType": null,
    "payerDocumentNumber": null,
    "expiresAt": null,
    "createdAt": "2026-08-07T15:00:00Z",
    "updatedAt": "2026-08-07T15:00:00Z"
  }
]
```
> Retorna array vazio `[]` se não houver pagamentos.

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
> **Campos obrigatórios:** `paymentId`.

**Response 200:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "REFUNDED",
  "gatewayTransactionId": "1234567890123456",
  "mpPaymentId": null,
  "qrCode": null,
  "qrCodeBase64": null,
  "ticketUrl": null,
  "payerEmail": null,
  "payerDocumentType": null,
  "payerDocumentNumber": null,
  "expiresAt": null,
  "createdAt": "2026-08-07T15:00:00Z",
  "updatedAt": "2026-08-07T15:10:00Z"
}
```

**Kafka Event:** `payment.failed` → `{ "paymentId": "uuid", "orderId": "uuid" }`

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 404 | PAYMENT_NOT_FOUND | Pagamento não encontrado |
| 409 | PAYMENT_ALREADY_PROCESSED | Pagamento já reembolsado |

---

## POST /api/v1/payments/webhook
**Purpose:** Receber notificações de status do Mercado Pago (IPN)
**Auth:** Nenhuma (público)

**Request (enviado pelo Mercado Pago):**
```json
{
  "id": 123456789,
  "type": "payment",
  "action": "payment.updated",
  "data": {
    "id": "123456789"
  }
}
```
> O serviço usa `data.id` para buscar o pagamento local via `gatewayTransactionId` e consulta o status atual no Mercado Pago.
> Sempre retorna `200 OK` para evitar reenvios.

---

## POST /api/v1/customers
**Purpose:** Criar um Customer no Mercado Pago e salvar um cartão (Customer + Card)
**Auth:** Bearer JWT

**Request:**
```json
{
  "email": "cliente@email.com",
  "firstName": "João",
  "lastName": "Silva",
  "documentType": "CPF",
  "documentNumber": "19119119100",
  "cardToken": "CARD_TOKEN_DO_MERCADOPAGO_JS",
  "paymentMethodId": "master"
}
```
> **Campos obrigatórios:** `email`, `cardToken`.
> `paymentMethodId` é opcional (bandeira: `visa`, `master`, `elo`, `amex`).

**Response 201:**
```json
{
  "customerId": "123456789-abcdef",
  "cardId": "987654321",
  "paymentMethodId": "master"
}
```
> Retorna apenas tokens (`customerId`, `cardId`). Nenhum dado de cartão é armazenado.

**Error responses:**
| Status | Code | Meaning |
|--------|------|---------|
| 400 | INVALID_INPUT | `email` ou `cardToken` ausentes |
| 401 | UNAUTHORIZED | Token ausente ou inválido |
| 502 | GATEWAY_UNAVAILABLE | Mercado Pago indisponível |

---

## POST /api/v1/customers/{customerId}/cards
**Purpose:** Adicionar/substituir o cartão salvo de um Customer (troca de cartão)
**Auth:** Bearer JWT

**Path param:** `customerId` (ID do Customer no Mercado Pago)

**Request:**
```json
{
  "cardToken": "NOVO_CARD_TOKEN",
  "paymentMethodId": "visa"
}
```
> **Campos obrigatórios:** `cardToken`.

**Response 201:**
```json
{
  "cardId": "1122334455",
  "paymentMethodId": "visa"
}
```

---

## DELETE /api/v1/customers/{customerId}/cards/{cardId}
**Purpose:** Remover um cartão salvo de um Customer (ex.: cancelamento de recorrência)
**Auth:** Bearer JWT

**Path params:** `customerId`, `cardId`

**Response 204:** No Content

---

## Cobrança recorrente (cartão salvo)

O `POST /api/v1/payments` também aceita `cardId` + `customerId` no lugar de `gatewayToken`,
para cobrar um cartão salvo (recorrência).

**Request (cartão salvo):**
```json
{
  "referenceId": "assinatura:123",
  "amount": 89.90,
  "paymentMethod": "CREDIT_CARD",
  "cardId": "987654321",
  "customerId": "123456789-abcdef",
  "installments": 1,
  "description": "Cobrança recorrente"
}
```
> Quando `cardId` está presente, o gateway usa `payer.type = "customer"` + `customerId`
> e não exige `gatewayToken`.

---

## Kafka Topics

### Producers (PaymentAPI → outros serviços)

| Tópico | Evento | Payload |
|--------|--------|---------|
| `payment.created` | Pagamento pendente criado | `{ "paymentId": "uuid", "orderId": "uuid" }` |
| `payment.completed` | Pagamento aprovado pelo gateway | `{ "paymentId": "uuid", "orderId": "uuid" }` |
| `payment.failed` | Pagamento reembolsado | `{ "paymentId": "uuid", "orderId": "uuid" }` |

### Consumers (outros serviços → PaymentAPI)

| Tópico | Evento | Ação | Status |
|--------|--------|------|--------|
| `order.created` | Novo pedido criado | Cria pagamento pendente automaticamente | ⚠️ Apenas log — a implementar |
| `order.cancelled` | Pedido cancelado | Reembolsa pagamento automaticamente | ⚠️ Apenas log — a implementar |

---

## Mercado Pago — Mapeamento de Status

| Status Mercado Pago | PaymentStatus local | Descrição |
|---------------------|---------------------|-----------|
| `approved` | `COMPLETED` | Pagamento aprovado |
| `rejected` | `FAILED` | Pagamento rejeitado |
| `in_process` | `PENDING` | Em análise |
| `pending` | `PENDING` | Aguardando pagamento |
| `refunded` | `REFUNDED` | Reembolsado |
| `cancelled` | `CANCELLED` | Cancelado |

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
