# PaymentAPI 💳

Microsserviço de gerenciamento de pagamentos do sistema de delivery.

## Stack

- **Java 17** + **Spring Boot 3.4**
- **PostgreSQL 16** + **Flyway** (migrations)
- **Apache Kafka** — mensageria (Spring Kafka 3.3)
- **JWT (HS256)** — autenticação (mesma chave da UserAPI)
- **Bucket4j** — rate limiting
- **Docker** + **Docker Compose**

## Como rodar

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
#   JWT_SECRET=...         (mesma chave da UserAPI em base64)

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
# Edite .env com sua chave:
#   JWT_SECRET=...

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
```

O perfil `dev` conecta em `localhost:5434/paymentdb` e `localhost:9092` (Kafka).

---

### Perfis disponíveis

| Perfil | Banco | Uso |
|--------|-------|-----|
| `dev` | PostgreSQL Docker (porta 5434) | Desenvolvimento local na IDE |
| `docker` | PostgreSQL Docker (hostname interno) | docker-compose |
| `prod` | PostgreSQL remoto | Produção |

---

## Documentação

- **API Contract:** `docs/api-contract.md`
- **SDD:** `docs/SDD-payment-api.md`
- **Spec (Gherkin):** `specs/payment.feature`
- **Swagger UI:** `http://localhost:8082/swagger-ui/index.html`

## Kafka Events

### Producer (PaymentAPI emite)

| Tópico | Quando |
|--------|--------|
| `payment.created` | Pagamento PENDING criado |
| `payment.completed` | Pagamento aprovado pelo gateway |
| `payment.failed` | Pagamento reembolsado |

### Consumer (PaymentAPI escuta)

| Tópico | Ação |
|--------|------|
| `order.created` | Cria pagamento PENDING automaticamente |
| `order.cancelled` | Reembolsa pagamento automaticamente |

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/v1/payments` | Criar pagamento pendente |
| POST | `/api/v1/payments/{id}/process` | Processar pagamento (gateway) |
| GET | `/api/v1/payments/{id}` | Buscar pagamento por ID |
| GET | `/api/v1/payments?userId=` | Listar pagamentos do usuário |
| POST | `/api/v1/payments/refund` | Reembolsar pagamento |

---

## Testes

```bash
./mvnw test                    # Testes unitários (70 testes)
./mvnw verify                  # Testes + JaCoCo (≥85% linha, ≥60% branch)
```

---

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters) — veja o [SDD](docs/SDD-payment-api.md) para detalhes.
