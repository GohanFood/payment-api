# PaymentAPI 💳

Microsserviço de gerenciamento de pagamentos do sistema de delivery com integração **Mercado Pago** (PIX + Cartão de Crédito/Débito).

## 🚀 Stack

- **Java 17** + **Spring Boot 3.4**
- **PostgreSQL 16** + **Flyway** (migrations)
- **Apache Kafka** — mensageria (Spring Kafka 3.3)
- **Mercado Pago SDK 2.8.0** — gateway de pagamento (PIX + Cartão)
- **JWT (HS256)** — autenticação (mesma chave da UserAPI)
- **Bucket4j** — rate limiting
- **Docker** + **Docker Compose**

## ⚡ Como rodar

### Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker e Docker Compose

---

### Opção 1 — Subir tudo junto (recomendado)

Usa o `docker-compose.yml` na **raiz do monorepo** (`deliveryAPI/`) para subir UserAPI + AddressesAPI + PaymentAPI + seus bancos + Kafka.

```bash
cd deliveryAPI

# 1. Configure o .env da raiz
cp payment-api/.env.example .env
# Edite .env com suas chaves:
#   JWT_SECRET=...
#   MERCADOPAGO_ACCESS_TOKEN=TEST-...  (sandbox) ou APP_USR-... (produção)
#   MERCADOPAGO_PUBLIC_KEY=TEST-...    (usada no frontend)

# 2. Suba todos os serviços
docker compose up -d
```

| Serviço | Porta |
|---------|-------|
| User API | `http://localhost:8080` |
| Addresses API | `http://localhost:8081` |
| Payment API | `http://localhost:8082` |
| Kafka | `localhost:9092` |

---

### Opção 2 — Subir só a PaymentAPI

```bash
cd payment-api

# 1. Configure as variáveis de ambiente
cp .env.example .env
# Edite .env:
#   JWT_SECRET=...
#   MERCADOPAGO_ACCESS_TOKEN=TEST-...  (opcional — sem token usa modo simulado)
#   MERCADOPAGO_PUBLIC_KEY=TEST-...

# 2. Suba a API + banco + Kafka
docker compose up -d
```

A API estará disponível em `http://localhost:8082`.

---

### Opção 3 — Rodar localmente (IDE)

Sobe o banco e Kafka no Docker e roda a aplicação pela IDE com perfil `dev`:

```bash
docker compose up -d postgres-payment kafka zookeeper
# Roda PaymentApplication pela IDE com spring.profiles.active=dev
# Opcional: configure MERCADOPAGO_ACCESS_TOKEN nas env vars da IDE
```

O perfil `dev` conecta em `localhost:5434/paymentdb` e `localhost:9092` (Kafka).
Sem `MERCADOPAGO_ACCESS_TOKEN`, o gateway opera em **modo simulado** (fallback automático).

---

### Perfis disponíveis

| Perfil | Banco | Uso |
|--------|-------|-----|
| `dev` | PostgreSQL Docker (porta 5434) | Desenvolvimento local na IDE |
| `docker` | PostgreSQL Docker (hostname interno) | docker-compose |
| `prod` | PostgreSQL remoto | Produção |

---

## 📚 Documentação

- **API Contract:** `docs/api-contract.md` — todos os endpoints com request/response
- **SDD:** `docs/SDD-payment-api.md` — design detalhado, arquitetura, fluxos
- **Spec (Gherkin):** `specs/payment.feature`
- **Swagger UI:** `http://localhost:8082/swagger-ui/index.html`
- **Postman Collection:** `postman-collection.json` — todos os endpoints prontos para testar

---

## 🧪 Testes

```bash
./mvnw test                    # Testes unitários (70 testes)
./mvnw verify                  # Testes + JaCoCo (≥85% linha, ≥60% branch)
```

### Datasets de teste do Mercado Pago (sandbox)

Use estes cartões no ambiente de testes do Mercado Pago:

| Cartão | Número | Bandeira | Comportamento |
|--------|--------|----------|---------------|
| Aprovado | 5031 4332 1540 6351 | Mastercard | Sempre aprova |
| Aprovado | 4235 6477 2802 5682 | Visa | Sempre aprova |
| Recusado | 4002 7636 7327 2116 | Visa | Fundos insuficientes |

**CVV:** `123` | **Vencimento:** `12/2030` | **Nome:** `APRO`

> ⚠️ Para gerar um `CardToken` use o endpoint `POST https://api.mercadopago.com/v1/card_tokens?public_key=<PUBLIC_KEY>` ou o MercadoPago.js no frontend.

---

## 🔗 Integrações

### Mercado Pago (Gateway de Pagamento)

| Método | Descrição |
|--------|-----------|
| **PIX** | QR Code gerado via `POST /v1/payments` com `paymentMethod: "PIX"` |
| **Cartão** | CardToken → `POST /v1/payments` com `gatewayToken` processa direto no MP |
| **Reprocessar** | `POST /v1/payments/{id}/process` para reenviar pagamento PENDING |
| **Reembolso** | Via `POST /v1/payments/refund` ou webhook (IPN) |

### Apache Kafka

| Direção | Tópico | Quando |
|---------|--------|--------|
| Producer | `payment.created` | Pagamento PENDING criado |
| Producer | `payment.completed` | Pagamento aprovado pelo MP |
| Producer | `payment.failed` | Pagamento reembolsado/rejeitado |
| Consumer | `order.created` | ⚠️ Apenas log — a implementar |
| Consumer | `order.cancelled` | ⚠️ Apenas log — a implementar |

---

## 📡 Endpoints

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/api/v1/payments` | Criar pagamento (PIX gera QR Code; Cartão com gatewayToken processa direto no MP) | JWT |
| POST | `/api/v1/payments/{id}/process` | Reprocessar pagamento PENDING via Mercado Pago (fallback) | JWT |
| GET | `/api/v1/payments/{id}` | Buscar pagamento por ID | JWT |
| GET | `/api/v1/payments` | Listar pagamentos do usuário autenticado | JWT |
| POST | `/api/v1/payments/refund` | Reembolsar pagamento | JWT |
| POST | `/api/v1/payments/webhook` | Webhook IPN Mercado Pago | — |
| POST | `/api/v1/customers` | Criar Customer + salvar cartão (Customer + Card) | JWT |
| POST | `/api/v1/customers/{id}/cards` | Adicionar/substituir cartão salvo | JWT |
| DELETE | `/api/v1/customers/{id}/cards/{cardId}` | Remover cartão salvo | JWT |

> **Nota:** O `userId` é extraído automaticamente do token JWT (campo `sub` ou `id`). Não é necessário enviar no body ou query params.
> **Cartão salvo (recorrência):** `POST /api/v1/payments` aceita `cardId` + `customerId` no lugar de `gatewayToken`, cobrando o cartão salvo sem novo `cardToken`.

### Exemplos de requisição

**PIX:**
```json
POST /api/v1/payments
{
  "orderId": "660e8400-e29b-41d4-a716-446655440002",
  "amount": 150.00,
  "paymentMethod": "PIX",
  "payerEmail": "test_user_123@testuser.com",
  "payerDocumentType": "CPF",
  "payerDocumentNumber": "19119119100"
}
```

**Cartão (MP direto):**
```json
POST /api/v1/payments
{
  "orderId": "660e8400-e29b-41d4-a716-446655440100",
  "amount": 89.90,
  "paymentMethod": "CREDIT_CARD",
  "payerEmail": "test_user_123@testuser.com",
  "payerDocumentType": "CPF",
  "payerDocumentNumber": "19119119100",
  "gatewayToken": "<card_token>",
  "paymentMethodId": "master",
  "installments": 1
}
```

---

## 🏗️ Arquitetura

Arquitetura Hexagonal (Ports & Adapters) — veja o [SDD](docs/SDD-payment-api.md) para detalhes.

```
adapter/in/web          ← PaymentController, JwtAuthenticationFilter
application/            ← UseCases, Services, DTOs, PaymentStatusSyncService
domain/                 ← Payment, PaymentStatus, GatewayPort, GatewayRequest/Response
adapter/out/            ←
  ├── gateway/          ← MercadoPagoGateway (PIX, Cartão, Reembolso, Consulta)
  ├── persistence/      ← JPA + Flyway + PostgreSQL
  └── messaging/        ← Kafka Producer/Consumer
```

## ⚙️ Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `JWT_SECRET` | Sim | Chave JWT em base64 (mesma da UserAPI) |
| `MERCADOPAGO_ACCESS_TOKEN` | Não* | Access token do MP (sem ele = modo simulado) |
| `MERCADOPAGO_PUBLIC_KEY` | Não | Public key para frontend MP.js |
| `MERCADOPAGO_ENVIRONMENT` | Não | `sandbox` (padrão) ou `production` |

\* Em produção, o token é obrigatório e deve ser `APP_USR-` com `MERCADOPAGO_ENVIRONMENT=production`.
