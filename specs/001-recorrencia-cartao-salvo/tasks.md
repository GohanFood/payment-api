# Tarefas — Recorrência com cartão salvo (payment-api)

> Derivado do [plan.md](./plan.md).

## 1. Domínio e persistência

- [ ] Criar `Customer` e `Card` (domínio) com valores mínimos (`mpCustomerId`, `mpCardId`,
      `paymentMethodId`, `lastFourDigits`).
- [ ] Criar `CustomerEntity` + `CardEntity` (JPA).
- [ ] Criar migration `V5__create_customers_and_cards.sql`.
- [ ] Criar `CustomerRepository` (port) + implementação JPA.
- [ ] Criar mappers de domínio ↔ entidade.

## 2. Gateway (MercadoPagoGateway)

- [ ] Adicionar `createCustomer(email)` → `mpCustomerId`.
- [ ] Adicionar `addCardToCustomer(customerId, cardToken)` → `mpCardId` + bandeira.
- [ ] Adicionar `removeCardFromCustomer(customerId, cardId)`.
- [ ] Estender `PaymentGatewayRequest` com `cardId` e `customerId` (opcionais).
- [ ] Estender `processCardPayment` para usar `cardId`/`customerId` (payer `type=customer`)
      quando `cardId` presente.
- [ ] Permitir `APP_USR-` (produção) quando `mercadopago.environment=production`.

## 3. Application (use cases)

- [ ] Criar `SaveCustomerUseCase` (create/reuse Customer + save Card).
- [ ] Criar `AddCardUseCase` (trocar cartão).
- [ ] Criar `RemoveCardUseCase`.
- [ ] Estender `CreatePaymentUseCase`/`CreatePaymentService` para aceitar `cardId`.
- [ ] Adicionar validação: cartão exige `gatewayToken` **ou** `cardId`.

## 4. API (adapter/in/web)

- [ ] Criar `POST /api/v1/customers`.
- [ ] Criar `POST /api/v1/customers/{id}/cards`.
- [ ] Criar `DELETE /api/v1/customers/{id}/cards/{cardId}`.
- [ ] Estender `PaymentController`/DTOs para aceitar `cardId`.
- [ ] Ajustar `JwtAuthenticationFilter` para popular principal com `claims.get("userId")`.

## 5. Callback HTTP

- [ ] Adicionar config `subscription.callback-url` / `subscription.callback-secret`.
- [ ] Criar `SubscriptionCallbackClient` (RestClient) com header `X-Webhook-Secret`.
- [ ] Disparar callback em `COMPLETED`/`REFUNDED` no `PaymentStatusSyncService`.
- [ ] Tornar callback idempotente e tolerante a falhas (log + retry simples).

## 6. Kafka opcional

- [ ] Tornar `KafkaConfig` condicional (`@ConditionalOnProperty`, ex. `kafka.enabled`).
- [ ] Garantir que `PaymentMessagingPort` tenha fallback no-op quando Kafka desabilitado.

## 7. Configuração

- [ ] Adicionar `subscription.callback-*` em `application.yml`/docker-compose.
- [ ] Documentar env vars (`SUBSCRIPTION_CALLBACK_URL`, `SUBSCRIPTION_CALLBACK_SECRET`).

## 8. Testes

- [ ] Unit: Customer/Card services (create/reuse, add, remove).
- [ ] Unit: `processCardPayment` com `cardId` (payer `type=customer`).
- [ ] Unit: validação `gatewayToken` vs `cardId`.
- [ ] Unit: callback dispara em `COMPLETED`/`REFUNDED` com header correto.
- [ ] Unit: `JwtAuthenticationFilter` popula principal com `userId`.
- [ ] Unit: `MercadoPagoGateway.init()` aceita produção quando `environment=production`.
- [ ] Integration: `POST /customers` idempotente (mesmo `userId` → reutiliza Customer).

## 9. Documentação

- [ ] Atualizar `docs/api-contract.md` com os endpoints de Customer/Card e `cardId`.
- [ ] Atualizar `README.md` (variáveis e fluxo de recorrência).
