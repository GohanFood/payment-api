# Estado atual — payment-api

> Baseline levantado em 2026-09-13, antes das mudanças de recorrência.

## Stack

- Java 17 + Spring Boot 3.4.4
- PostgreSQL 16 + Flyway (migrations `V1`..`V4`)
- Mercado Pago SDK 2.8.0 (PIX + cartão)
- JWT (HS256) — autenticação via `Authorization: Bearer`
- Apache Kafka (Spring Kafka) — mensageria
- Bucket4j — rate limiting
- Docker + Docker Compose

## Pacotes (arquitetura hexagonal)

```
adapter/in/web          ← PaymentController, JwtAuthenticationFilter
application/            ← UseCases, Services, DTOs, PaymentStatusSyncService
domain/payment/         ← Payment, PaymentStatus, GatewayPort, GatewayRequest/Response
adapter/out/
  ├── gateway/          ← MercadoPagoGateway (PIX, Cartão, Reembolso, Consulta)
  ├── persistence/      ← JPA + Flyway + PostgreSQL
  └── messaging/        ← Kafka Producer/Consumer
port/                   ← PaymentRepository, PaymentGatewayPort, PaymentMessagingPort
```

## Endpoints atuais (`/api/v1/payments`)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/` | JWT | Criar pagamento (PIX gera QR; cartão processa com `gatewayToken`) |
| POST | `/{id}/process` | JWT | Reprocessar pagamento PENDING (exige `ProcessPaymentRequest`) |
| GET | `/{id}` | JWT | Buscar pagamento |
| GET | `/` | JWT | Listar pagamentos do usuário |
| POST | `/refund` | JWT | Reembolsar |
| POST | `/webhook` | — | Webhook IPN do Mercado Pago |

## Contrato atual de criação (`POST /api/v1/payments`)

Request (`CreatePaymentRequest`):
- `referenceId` (String, obrigatório)
- `amount` (BigDecimal, obrigatório, > 0)
- `paymentMethod` (String, obrigatório — `PIX` / `CREDIT_CARD` / `DEBIT_CARD`)
- `payerEmail`, `payerDocumentType`, `payerDocumentNumber`
- `gatewayToken` (cardToken, usado no cartão)
- `installments` (default 1)
- `paymentMethodId` (bandeira: `visa`, `master`, `elo`, `amex`)
- `issuerId`
- `description`

Response (`PaymentResponse`):
- `id` (UUID), `userId`, `referenceId`, `amount`, `paymentMethod`, `status`,
  `gatewayTransactionId`, `mpPaymentId`, `qrCode`, `qrCodeBase64`, `ticketUrl`,
  `payerEmail`, `payerDocumentType`, `payerDocumentNumber`, `expiresAt`,
  `createdAt`, `updatedAt`

## Domínio

`PaymentStatus` (enum): `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED`, `CANCELLED`.

`Payment` (domínio): `id`, `userId`, `referenceId`, `amount`, `paymentMethod`, `status`,
`gatewayTransactionId`, `mpPaymentId`, `qrCode`, `qrCodeBase64`, `ticketUrl`,
`payerEmail`, `payerDocumentType`, `payerDocumentNumber`, `expiresAt`, `createdAt`, `updatedAt`.

Métodos de estado: `markAsProcessing`, `markAsCompleted(gatewayTransactionId)`,
`markAsFailed`, `markAsRefunded`, `linkMercadoPago(...)`, `isPix()`, `isPending()`.

## Gateway (MercadoPagoGateway)

- `createPixPayment(...)` → `PixPaymentResponse` (`mpPaymentId`, `status`, `qrCode`,
  `qrCodeBase64`, `ticketUrl`, `dateOfExpiration`).
- `processCardPayment(PaymentGatewayRequest)` → `PaymentGatewayResponse`
  (`externalId`, `externalStatus`, `externalStatusDetail`, `paymentTypeId`,
  `dateApproved`, `paymentMethodId`, `installments`), com `isApproved()`,
  `isRejected()`, `isPending()`, `isRefunded()`.
- `refundPayment(gatewayPaymentId)` e `getPayment(gatewayPaymentId)`.
- `init()` exige `TEST-` (sandbox) e **rejeita** `APP_USR-` (produção) lançando
  `IllegalStateException`.

## Persistência / migrations

- `V1__create_payments_table.sql` — tabela `payments` (`user_id UUID`, `order_id UUID`).
- `V2__add_mercadopago_fields.sql` — `mp_payment_id`, `qr_code`, `qr_code_base64`,
  `ticket_url`, `payer_email`, `payer_document_type`, `payer_document_number`, `expires_at`.
- `V3__change_user_id_to_varchar.sql` — `user_id` → `VARCHAR(255)`.
- `V4__change_order_id_to_reference_id.sql` — `order_id` → `reference_id` `VARCHAR(255)`.

## JWT

`JwtAuthenticationFilter` lê o token com a chave `jwt.secret`, extrai `sub` (ou claim `id`)
e o `role`, e popula `UsernamePasswordAuthenticationToken` com o principal = `subject`.
O `PaymentController.getCurrentUserId()` usa `auth.getName()` (o `sub`).

> ⚠️ O sistema consumidor assina com `sub=username` e claim `userId` (numérica). Para a
> integração funcionar, o filtro precisa popular o principal com
> `claims.get("userId").toString()`.

## Kafka

`KafkaConfig` é `@Profile("!test")` (sempre ativo fora de teste). Produtores:
`payment.created`, `payment.completed`, `payment.failed`, `payment.refunded`.

## Mercado Pago (config)

`MercadoPagoProperties` (prefixo `mercadopago`): `environment` (default `sandbox`),
`access-token`, `public-key`, `webhook-secret`.

`application.yml`:
- `jwt.secret=${JWT_SECRET}`
- `mercadopago.access-token=${MERCADOPAGO_ACCESS_TOKEN:}`
- `mercadopago.public-key=${MERCADOPAGO_PUBLIC_KEY:}`
- `mercadopago.webhook-secret=${MERCADOPAGO_WEBHOOK_SECRET:}`
- `server.port=8082`
- Flyway habilitado.
