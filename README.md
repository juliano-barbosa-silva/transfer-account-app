# 💳 Fintech Account Service

Projeto demonstrativo de arquitetura **Senior/Staff Backend Java** no contexto de fintech.

A proposta não é apenas "rodar", mas demonstrar boas práticas reais de engenharia:

- Arquitetura limpa e separação de responsabilidades
- Domínio desacoplado da infraestrutura
- Idempotência em operações financeiras
- Eventos assíncronos com Kafka
- Observabilidade com métricas e rastreamento
- Preparação para escala

---

## 🛠️ Stack

| Tecnologia | Objetivo |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 3 | Framework |
| Spring Data JPA | Persistência |
| PostgreSQL | Banco de dados relacional |
| Kafka | Mensageria e eventos |
| Lombok | Redução de boilerplate |
| Testcontainers | Testes de integração |
| Flyway | Migrations de banco |
| Micrometer | Métricas e observabilidade |

---

## 📁 Estrutura do Projeto

```
src/main/java
├── application
│   ├── dto               # Objetos de transferência de dados (Request/Response)
│   ├── service           # Serviços de aplicação (orquestração)
│   └── usecase           # Casos de uso (regras de negócio)
│
├── domain
│   ├── account           # Entidade e regras de conta
│   ├── transaction       # Entidade e regras de transação
│   ├── event             # Eventos de domínio
│   └── exception         # Exceções de negócio
│
├── infrastructure
│   ├── persistence       # Repositórios JPA e entidades
│   ├── kafka             # Producers e consumers de eventos
│   └── config            # Configurações (Kafka, DataSource, etc.)
│
├── api
│   ├── controller        # Endpoints REST
│   └── advice            # Tratamento global de exceções
│
└── AccountApplication.java
```

---

## 🏛️ Decisões de Arquitetura

### Separação de Camadas

O projeto segue os princípios da **Clean Architecture**, onde o domínio não conhece a infraestrutura:

- `domain` — sem dependências externas; só regras de negócio puras
- `application` — orquestra casos de uso; depende apenas do domínio
- `infrastructure` — implementa interfaces definidas no domínio (repositórios, clientes externos)
- `api` — ponto de entrada HTTP; delega para a camada de aplicação

### Idempotência

Transações financeiras implementam idempotência via `idempotencyKey`:

- Toda operação recebe uma chave única do cliente
- O campo possui constraint `UNIQUE` no banco
- Requisições duplicadas retornam o resultado original sem reprocessar

### Eventos de Domínio

Operações relevantes (ex: `TransactionCreated`, `BalanceUpdated`) são publicadas no Kafka, permitindo:

- Desacoplamento entre serviços
- Auditoria assíncrona
- Reprocessamento em caso de falha

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- Docker e Docker Compose

### Subindo a infraestrutura

```bash
docker-compose up -d
```

O `docker-compose.yml` provisiona PostgreSQL e Kafka localmente.

### Rodando a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta `8080` por padrão.

---

## 🔌 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/accounts` | Cria uma nova conta |
| `GET` | `/accounts/{id}` | Consulta saldo e dados da conta |
| `POST` | `/accounts/{id}/transactions` | Realiza uma transação (débito/crédito) |
| `GET` | `/accounts/{id}/transactions` | Lista transações da conta |

### Exemplo de Transação

```http
POST /accounts/123e4567-e89b-12d3-a456-426614174000/transactions
Content-Type: application/json
Idempotency-Key: pedido-abc-001

{
  "type": "DEBIT",
  "amount": 150.00
}
```

---

## 🧪 Testes

O projeto usa **Testcontainers** para subir PostgreSQL e Kafka reais durante os testes de integração, garantindo fidelidade ao ambiente de produção.

```bash
# Testes unitários
./mvnw test

# Testes de integração
./mvnw verify
```

---

## 📊 Observabilidade

Métricas expostas via **Micrometer** no endpoint `/actuator/metrics`, compatíveis com Prometheus e Grafana.

Principais métricas monitoradas:

- Volume de transações por tipo
- Taxa de erros e rejeições
- Latência dos endpoints
- Lag de consumidores Kafka

---

## 🗄️ Migrations

As migrations de banco são gerenciadas pelo **Flyway** e ficam em:

```
src/main/resources/db/migration/
├── V1__create_accounts_table.sql
├── V2__create_transactions_table.sql
└── V3__add_idempotency_index.sql
```

---

## 📌 Boas Práticas Aplicadas

- Sem lógica de negócio em controllers ou entidades JPA
- Exceptions de domínio mapeadas para HTTP status via `@ControllerAdvice`
- Transações bancárias sempre dentro de `@Transactional`
- BigDecimal para todos os valores monetários (nunca `double` ou `float`)
- UUIDs como identificadores para evitar enumeração e facilitar distribuição