# Plano técnico — Recorrência com cartão salvo (payment-api)

> Vinculado a: [spec.md](./spec.md) · [current-state.md](./current-state.md) · [tasks.md](./tasks.md)

## 0. Princípios

- **Nunca armazenar/logar dados de cartão.** Só tokens do Mercado Pago:
  `cardToken` (checkout), `customerId` + `cardId` (recorrência).
- Manter a arquitetura hexagonal atual (ports & adapters).
- Manter o contrato pontual existente (PIX e cartão com `cardToken`), adicionando
  `cardId` como alternativa de pagamento com cartão salvo.
- `userId` sempre vem do token JWT (nunca do body).

## 1. Modelo de dados

### 1.1 Tabela `customers` (nova) — mapeia usuário → Customer do Mercado Pago

| Coluna | Tipo | Observação |
|---|---|---|
| `id` | UUID PK | id interno |
| `user_id` | VARCHAR | id do usuário no sistema consumidor (claim JWT) |
| `mp_customer_id` | VARCHAR | ID do Customer no Mercado Pago (token) |
| `default_card_id` | VARCHAR | cartão padrão no MP (token) |
| `created_at` | TIMESTAMP | — |
| `updated_at` | TIMESTAMP | — |

- `user_id` único (1 Customer por usuário).

### 1.2 Tabela `cards` (nova) — cartões salvos por Customer

| Coluna | Tipo | Observação |
|---|---|---|
| `id` | UUID PK | id interno |
| `customer_id` | UUID FK | → `customers.id` |
| `mp_card_id` | VARCHAR | ID do cartão no MP (token) |
| `payment_method_id` | VARCHAR | bandeira (`visa`, `master`, etc.) |
| `last_four_digits` | VARCHAR(4) | últimos 4 dígitos (exibição, não é dado sensível) |
| `created_at` | TIMESTAMP | — |

> `last_four_digits` é apenas para UX (mostrar "termina em 1234"); não viola PCI.

### 1.3 Migration

- `V5__create_customers_and_cards.sql`

## 2. Endpoints novos

### 2.1 `POST /api/v1/customers`

Request:
```json
{ "cardToken": "<token do CardForm>", "payerEmail": "..." }
```
Response `201`:
```json
{ "customerId": "...", "cardId": "...", "paymentMethodId": "master" }
```

- Extrai `userId` do JWT.
- Se já existe `customers` para o `userId`, reutiliza e adiciona o cartão.
- Cria Customer no MP (`POST /v1/customers`) com `email` do payer, depois associa o
  cartão (`POST /v1/customers/{id}/cards` com `cardToken`).

### 2.2 `POST /api/v1/customers/{id}/cards`

Request:
```json
{ "cardToken": "<novo token>" }
```
Response `201`:
```json
{ "cardId": "...", "paymentMethodId": "visa" }
```

- Adiciona o cartão e o marca como `default_card_id` do Customer.

### 2.3 `DELETE /api/v1/customers/{id}/cards/{cardId}`

- Remove o cartão no MP e localmente.
- Se era o cartão padrão, limpa `default_card_id` (ou promove outro, se houver).

## 3. Cobrança recorrente com `cardId`

### 3.1 Estender `CreatePaymentRequest`

Adicionar campo opcional:
```java
/** Cartão salvo (recorrência). Alternativa a gatewayToken. */
private String cardId;
```

Regra de validação:
- Cartão (`CREDIT_CARD`/`DEBIT_CARD`) exige **ou** `gatewayToken` **ou** `cardId`.
- Se `cardId` presente → cobrança com `payer.type = "customer"` e `customerId`.

### 3.2 `MercadoPagoGateway.processCardPayment`

Estender `PaymentGatewayRequest` com `cardId` e `customerId` (opcionais). Quando presentes:
- `PaymentPayerRequest`: `.type("customer").id(customerId)`.
- `PaymentCreateRequest`: usar `cardId` em vez de `token`.

> No SDK do Mercado Pago, o pagamento com cartão salvo usa o `card_id` (e o `payer`
> com `type: customer`), não um novo `cardToken`.

### 3.3 Idempotência

- Manter `x-idempotency-key` = `payment.getId().toString()` (já usado).
- Para recorrência, o sistema consumidor monta a chave por ciclo
  (`referencia + periodo`) — o contrato continua transparente para o payment-api.

## 4. Callback HTTP para o sistema consumidor

- Configurar:
  - `SUBSCRIPTION_CALLBACK_URL` (ex.: `http://consumidor:8080/pagamentos/webhook`)
  - `SUBSCRIPTION_CALLBACK_SECRET` (header `X-Webhook-Secret`)
- Disparar em `payment.completed` e `payment.refunded` (no `PaymentStatusSyncService`,
  ao lado do `publishPaymentCompleted/Refunded`).
- Payload (idempotente):
```json
{
  "referenceId": "pedido:123",
  "paymentId": "<uuid payment-api>",
  "status": "COMPLETED" | "REFUNDED",
  "paymentMethod": "PIX" | "CARD",
  "amount": 89.90,
  "occurredAt": "2026-08-30T12:00:00Z"
}
```
- `X-Webhook-Secret` = `SUBSCRIPTION_CALLBACK_SECRET` (validado pelo sistema consumidor).

## 5. JWT — compatibilidade com o sistema consumidor

- `JwtAuthenticationFilter` deve popular o principal com `claims.get("userId").toString()`
  (claim numérica), com fallback para `sub`/`id`.
- Manter `getCurrentUserId()` usando `auth.getName()`.

## 6. Mercado Pago produção

- `MercadoPagoGateway.init()` deve aceitar `APP_USR-` quando `mercadopago.environment=production`.
- Sandbox: `TEST-` obrigatório (comportamento atual).
- Sem token (`isTokenConfigured() == false`): manter aviso e lançar exceção ao usar.

## 7. Kafka opcional

- `KafkaConfig` deve ser condicional (`@ConditionalOnProperty` / flag `kafka.enabled`),
  para permitir rodar sem broker quando o callback HTTP for suficiente.

## 8. Config (`application.yml`)

```yaml
subscription:
  callback-url: ${SUBSCRIPTION_CALLBACK_URL:}
  callback-secret: ${SUBSCRIPTION_CALLBACK_SECRET:}
```

## 9. Testes

- Unit: Customer/Card service (cria/atualiza/remove, mapeamento de tokens).
- Unit: `processCardPayment` com `cardId` (monta payer `type=customer`).
- Unit: validação `gatewayToken` vs `cardId`.
- Unit/Integration: callback HTTP dispara em `COMPLETED`/`REFUNDED` com header correto.
- Unit: `JwtAuthenticationFilter` popula principal com `userId`.
- Unit: `MercadoPagoGateway.init()` aceita produção quando `environment=production`.
- Integration: `POST /customers` idempotente (mesmo `userId` → reutiliza Customer).

## 10. Riscos

- **Dupla cobrança** → mitigado por idempotency key estável por ciclo.
- **Dessincronia de userId** → mesma claim JWT (`userId`) entre sistema consumidor e payment-api.
- **Vazamento de dados de cartão** → só tokens; nunca logar `cardToken`/payload sensível.
- **Quebra de contrato existente** → manter `gatewayToken` funcionando; `cardId` é aditivo.
