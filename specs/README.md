# Specs — payment-api

Diretório de especificações seguindo o estilo [Spec Kit](https://github.com/github/spec-kit).

## Índice

| Número | Título | Status |
|---|---|---|
| 001 | [Recorrência de pagamento com cartão salvo (Customer + Card)](./001-recorrencia-cartao-salvo/spec.md) | Em andamento |

## Estrutura

Cada especificação possui:

- `spec.md` — o **quê** e o **porquê** (contexto, user stories, requisitos).
- `plan.md` — o **como** técnico (arquitetura, endpoints, decisões).
- `tasks.md` — lista de tarefas acionáveis para implementação.
- `current-state.md` — baseline do estado atual (opcional, útil para adaptações).

## Observação

O arquivo `payment.feature` (Gherkin) na raiz de `specs/` é uma especificação de
comportamento existente; as novas features seguirão a estrutura Spec Kit em pastas
numeradas.
