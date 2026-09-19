# 001 — Recorrência de pagamento com cartão salvo (Customer + Card no Mercado Pago)

## Contexto

O `payment-api` hoje processa pagamentos pontuais via Mercado Pago:
- **PIX** — cria pagamento e devolve QR Code.
- **Cartão** — recebe um `cardToken` (uso único, gerado pelo MercadoPago.js no frontend)
  e processa imediatamente.

Para viabilizar modelos de receita recorrente (ex.: assinatura com período de teste e
cobrança mensal), o `payment-api` precisa passar a suportar **cartão salvo**, permitindo
cobrar novamente no futuro sem que o cliente digite o cartão de novo e **sem armazenar
dados de cartão** no backend.

## Objetivos de negócio

- Salvar um cartão no Mercado Pago (Customer + Card) a partir de um `cardToken`, devolvendo
  apenas tokens (`customerId`, `cardId`).
- Permitir **cobrança recorrente** usando o `cardId` salvo — sem novo `cardToken` e sem
  dados de cartão no fluxo.
- Permitir **atualizar/trocar** o cartão salvo para o próximo ciclo.
- Permitir **remover** o cartão salvo (cancelamento de recorrência).
- Manter o fluxo pontual atual (PIX e cartão com `cardToken`) sem quebrar contratos.

## Personas

- **Sistema consumidor (backend)** — integra com o payment-api para salvar cartões e
  disparar cobranças recorrentes (ex.: serviço de assinaturas do Gohan Food).
- **Usuário final** — titular do cartão; interage apenas via frontend do sistema consumidor.
- **Admin** — precisa rastrear pagamentos e status no Mercado Pago.

## Requisitos funcionais

- RF-01: `POST /api/v1/customers` cria um Customer e associa um cartão a partir de um
  `cardToken`, retornando `customerId`, `cardId` e `paymentMethodId` (bandeira).
  - Se já existir Customer para o mesmo `userId`, reutiliza o Customer e adiciona o cartão.
- RF-02: `POST /api/v1/customers/{id}/cards` adiciona/troca o cartão a partir de um novo
  `cardToken`, retornando o novo `cardId` (que passa a ser o cartão padrão).
- RF-03: `DELETE /api/v1/customers/{id}/cards/{cardId}` remove o cartão salvo.
- RF-04: `POST /api/v1/payments` deve aceitar **`cardId`** (em vez de `gatewayToken`) para
  criar cobrança recorrente com `payer.type = "customer"` + `customerId`.
- RF-05: Cobrança recorrente segue o mesmo contrato de status atual:
  `approved → COMPLETED`, `rejected → FAILED`, `pending/in_process → PENDING`.
- RF-06: O `userId` deve continuar vindo do token JWT (não do body).
- RF-07: Toda cobrança recorrente deve ser **idempotente** (chave de idempotência estável
  por ciclo), evitando cobrança duplicada.
- RF-08: Ao confirmar/estornar um pagamento (webhook do Mercado Pago), o `payment-api` deve
  notificar o sistema consumidor via **callback HTTP** com header `X-Webhook-Secret`.

## Requisitos não funcionais

- RNF-01: **Nenhum dado de cartão** (número, CVV, validade) pode ser armazenado ou logado.
  Apenas tokens do Mercado Pago (`cardToken`, `customerId`, `cardId`).
- RNF-02: Manter compatibilidade com o JWT do sistema consumidor (claim `userId`, com
  fallback para `sub`/`id`).
- RNF-03: Suportar sandbox (`TEST-`) e produção (`APP_USR-`) de forma explícita por ambiente.
- RNF-04: Kafka deve continuar opcional (flag), pois o callback HTTP é o mecanismo primário
  de notificação para o sistema consumidor.
- RNF-05: Idempotência nas operações de criação de Customer/Card e nas cobranças.

## Fora de escopo

- Gestão de planos/preços (responsabilidade do sistema consumidor).
- Reembolso automático / disputas (chargebacks) — continua manual.
- Pré-autorização com captura posterior (`capture=false`) — substituída por cartão salvo.
- Migração de dados de cartão de sistemas legados (não existem).
