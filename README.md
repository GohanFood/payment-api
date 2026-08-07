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
| Cartão | Número | CVV | Vencimento |
|--------|--------|-----|------------|
| Aprovado | 5031 4332 1540 6351 | 123 | 11/25 |
| Recusado | 5031 4332 1540 6351 | 123 | 11/25 (valor > aprovação) |
| Pendente | 5031 4332 1540 6351 | 123 | 11/25 |

---

## 🔗 Integrações

### Mercado Pago (Gateway de Pagamento)

| Método | Descrição |
|--------|-----------|
| **PIX** | QR Code gerado via `POST /v1/payments` com `paymentMethod: "PIX"` |
| **Cartão** | CardToken via MercadoPago.js CardForm → `POST /v1/payments/{id}/process` |
| **Reembolso** | Via `POST /v1/payments/refund` ou `POST /v1/payments/webhook` (IPN) |

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
| POST | `/api/v1/payments` | Criar pagamento (PIX gera QR Code) | JWT |
| POST | `/api/v1/payments/{id}/process` | Processar via cartão (Mercado Pago) | JWT |
| GET | `/api/v1/payments/{id}` | Buscar pagamento por ID | JWT |
| GET | `/api/v1/payments?userId=` | Listar pagamentos do usuário | JWT |
| POST | `/api/v1/payments/refund` | Reembolsar pagamento | JWT |
| POST | `/api/v1/payments/webhook` | Webhook IPN Mercado Pago | — |

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

\* Em produção, o token é obrigatório.
