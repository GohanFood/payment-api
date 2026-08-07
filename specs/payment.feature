@draft
Feature: Gestão de Pagamentos (PaymentAPI)

  Como um comprador do sistema de delivery
  Quero realizar o pagamento dos meus pedidos
  Para finalizar a compra e receber meu pedido

  Context:
    Given o microsserviço PaymentAPI está operacional
    And o banco de dados de pagamentos está acessível
    And o broker Kafka está operacional para eventos de pagamento
    And o gateway de pagamento está em modo Sandbox

  # ============================================================
  # P1 - Funcionalidades Críticas
  # ============================================================

  @P1
  Scenario: Criar pagamento pendente com sucesso
    Given que existe um usuário autenticado com id "550e8400-e29b-41d4-a716-446655440001"
    And existe um pedido com id "660e8400-e29b-41d4-a716-446655440002"
    When o usuário cria um pagamento de R$ 150,00 com método "CREDIT_CARD"
    Then o sistema retorna status 201
    And a resposta contém id, userId, orderId, amount, paymentMethod e status "PENDING"
    And o campo gatewayTransactionId é null
    And um evento "payment.created" é publicado no Kafka com paymentId e orderId

  @P1
  Scenario: Tentar criar pagamento com valor zero ou negativo
    Given que existe um usuário autenticado
    When o usuário tenta criar um pagamento de R$ 0,00
    Then o sistema retorna status 400
    And a resposta contém o erro "INVALID_PAYMENT_AMOUNT"

  @P1
  Scenario: Tentar criar pagamento sem token de autenticação
    Given que não há token de autenticação
    When o usuário tenta criar um pagamento
    Then o sistema retorna status 401
    And a resposta contém o erro "UNAUTHORIZED"

  @P1
  Scenario: Processar pagamento pendente com sucesso
    Given que existe um pagamento pendente de id "770e8400-e29b-41d4-a716-446655440003"
    And o gateway de pagamento aprova a transação e retorna token "GW-abc123def456"
    When o usuário processa o pagamento com gatewayToken válido
    Then o sistema retorna status 200
    And o status do pagamento muda para "COMPLETED"
    And o gatewayTransactionId é preenchido com "GW-abc123def456"
    And um evento "payment.completed" é publicado no Kafka

  @P1
  Scenario: Tentar processar pagamento já concluído
    Given que existe um pagamento com status "COMPLETED"
    When o usuário tenta processar novamente este pagamento
    Then o sistema retorna status 409
    And a resposta contém o erro "PAYMENT_ALREADY_PROCESSED"

  @P1
  Scenario: Tentar processar pagamento inexistente
    Given que existe um usuário autenticado
    When o usuário tenta processar um pagamento com id inexistente
    Then o sistema retorna status 404
    And a resposta contém o erro "PAYMENT_NOT_FOUND"

  @P1
  Scenario: Buscar pagamento por ID com sucesso
    Given que existe um pagamento concluído de id "770e8400-e29b-41d4-a716-446655440003"
    When o usuário solicita o pagamento pelo id
    Then o sistema retorna status 200
    And a resposta contém todos os dados do pagamento incluindo gatewayTransactionId

  @P1
  Scenario: Listar pagamentos do usuário
    Given que existe um usuário autenticado com 3 pagamentos
    When o usuário solicita a lista de pagamentos
    Then o sistema retorna status 200
    And a resposta contém uma lista com 3 pagamentos

  @P1
  Scenario: Reembolsar pagamento concluído
    Given que existe um pagamento com status "COMPLETED"
    When o usuário solicita o reembolso deste pagamento
    Then o sistema retorna status 200
    And o status do pagamento muda para "REFUNDED"
    And um evento "payment.failed" é publicado no Kafka

  @P1
  Scenario: Processar pagamento com gatewayToken inválido
    Given que existe um pagamento pendente
    And o gateway de pagamento rejeita o token como inválido
    When o usuário processa o pagamento com gatewayToken inválido
    Then o sistema retorna status 502
    And a resposta contém o erro "GATEWAY_UNAVAILABLE"

  # ============================================================
  # P2 - Funcionalidades Secundárias
  # ============================================================

  @P2
  Scenario: Tentar criar pagamento sem campos obrigatórios
    Given que existe um usuário autenticado
    When o usuário tenta criar um pagamento sem o campo "orderId"
    Then o sistema retorna status 400
    And a resposta contém o erro "INVALID_INPUT"

  @P2
  Scenario: Listar pagamentos de usuário sem pagamentos
    Given que existe um usuário autenticado sem pagamentos
    When o usuário solicita a lista de pagamentos
    Then o sistema retorna status 200
    And a resposta contém uma lista vazia

  @P2
  Scenario: Ordem cancelada aciona reembolso automático via Kafka
    Given que existe um pagamento com status "COMPLETED"
    When o tópico "order.cancelled" publica evento com o orderId do pagamento
    Then o PaymentAPI consome o evento via KafkaConsumer
    And o status do pagamento muda para "REFUNDED"
    And um evento "payment.failed" é publicado no Kafka

  @P2
  Scenario: Criação de pedido gera intenção de pagamento via Kafka
    Given que o tópico "order.created" publica evento com orderId e userId
    When o PaymentAPI consome o evento via KafkaConsumer
    Then um pagamento pendente é criado automaticamente para o pedido

  @P2
  Scenario: Tentar reembolsar pagamento já reembolsado
    Given que existe um pagamento com status "REFUNDED"
    When o usuário tenta solicitar o reembolso deste pagamento
    Then o sistema retorna status 409
    And a resposta contém o erro "PAYMENT_ALREADY_PROCESSED"

  # ============================================================
  # P3+ - Funcionalidades Futuras
  # ============================================================

  @P3
  Scenario: Rate limit nos endpoints de pagamento
    Given que existe um usuário autenticado
    When o usuário tenta criar mais de 10 pagamentos em 1 minuto
    Then o sistema retorna status 429
    And a resposta contém o erro "TOO_MANY_REQUESTS"

  @P3
  Scenario: Idempotência na criação de pagamento
    Given que existe um usuário autenticado
    When o usuário tenta criar um pagamento com o mesmo Idempotency-Key duas vezes
    Then o sistema retorna status 200 na segunda chamada
    And a resposta é a mesma do primeiro pagamento criado [NEEDS CLARIFICATION: ou 409?]

  @P3
  Scenario: Webhook do gateway de pagamento confirma pagamento assíncrono
    Given que existe um pagamento com status "PROCESSING"
    When o gateway de pagamento envia webhook confirmando aprovação
    Then o sistema atualiza o status para "COMPLETED"
    And um evento "payment.completed" é publicado no Kafka
    And uma notificação é enviada ao usuário [NEEDS CLARIFICATION: canal de notificação]

  @P3
  Scenario: Suporte a múltiplos métodos de pagamento
    Given que existe um usuário autenticado
    When o usuário cria um pagamento com método "PIX"
    Then o sistema retorna status 201
    And o campo paymentMethod na resposta é "PIX"
    And o status inicia como "PENDING"

  @P3
  Scenario: Tentativa de pagamento com valor acima do limite
    Given que existe um usuário autenticado
    When o usuário tenta criar um pagamento de R$ 10.000,00
    Then o sistema retorna status 422
    And a resposta contém o erro "AMOUNT_EXCEEDS_LIMIT"

  @P3
  Scenario: Circuit breaker para gateway de pagamento indisponível
    Given que o gateway de pagamento está indisponível por mais de 5 tentativas
    When o usuário tenta processar um pagamento
    Then o sistema retorna status 503
    And a resposta contém o erro "SERVICE_UNAVAILABLE"
    And o circuito é aberto por 30 segundos
