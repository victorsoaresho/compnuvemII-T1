# compnuvemII-T1 — Banco local e consumidor de mensagens

Este documento descreve como subir o banco de dados com Docker Compose e como o projeto `backend-v1` consome mensagens do Google Cloud Pub/Sub.

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e Docker Compose
- Java 17 e Maven (para executar o `backend-v1`)

---

## Banco de dados (PostgreSQL)

O arquivo [`database/docker-compose.yaml`](database/docker-compose.yaml) sobe um **PostgreSQL 16** com:

| Item | Valor |
|------|--------|
| Imagem | `postgres:16-alpine` |
| Container | `compnuvem-t1-postgres` |
| Porta host | `5432` |
| Usuário / senha | `postgres` / `postgres` |
| Banco | `compnuvem-t1` |

### Como subir

No terminal, a partir da pasta `database`:

```bash
cd database
docker compose up -d
```

Para ver os logs:

```bash
docker compose logs -f postgres
```

Para parar e remover os containers (os dados em volume permanecem em `database/data`):

```bash
docker compose down
```

### Inicialização do schema

Scripts em [`database/init/`](database/init/) são montados em `/docker-entrypoint-initdb.d` e rodam **na primeira criação** do volume de dados. O arquivo [`database/init/tables.sql`](database/init/tables.sql) cria as tabelas do domínio (cliente, pedido, produto, etc.).

Se você já subiu o container antes e o volume `database/data` já existia, alterações em `init/` não serão reaplicadas automaticamente — nesse caso é preciso remover o volume (apaga os dados) ou aplicar migrações manualmente.

### Healthcheck

O serviço expõe healthcheck com `pg_isready`, útil para orquestrar dependências em outros compose ou pipelines.

---

## Backend (`backend-v1`) — consumo de mensagens

O `backend-v1` é uma aplicação **Spring Boot** sem servidor HTTP cuja classe principal é `MessagingConsumerApplication`. Ela assina uma **subscription** do **Google Cloud Pub/Sub** e processa mensagens em streaming.

### Conexão com o PostgreSQL

O [`application.properties`](backend-v1/src/main/resources/application.properties) usa por padrão:

- URL: `jdbc:postgresql://localhost:5432/compnuvem-t1`
- Usuário: `postgres`
- Senha: `postgres`

Você pode sobrescrever com variáveis de ambiente:

| Variável | Descrição |
|----------|-----------|
| `DATABASE_URL` | JDBC URL completa |
| `DB_USER` | usuário |
| `DB_PASSWORD` | senha |
| `JPA_DDL_AUTO` | modo Hibernate (padrão `none`) |

Suba o Postgres (seção anterior) antes de rodar o consumer. O schema das tabelas e criado automaticamente pelo script `database/init/tables.sql` na primeira vez que o container sobe.

### Google Cloud Pub/Sub — como funciona

1. **Ativação** — Com `app.pubsub.enabled=true` (padrão), o Spring registra beans que carregam credenciais e iniciam o assinante. Com `false`, o subscriber não é criado (útil para testes ou ambiente sem GCP).

2. **Credenciais** — O caminho padrão é `file:./credentials/credentials.json` (relativo ao diretório de trabalho ao executar o JAR, em geral a pasta `backend-v1`). O arquivo deve ser um JSON de **service account** no formato Google, com:
   - `project_id` obrigatório;
   - bloco opcional `pubsub` com pelo menos `subscription` (ID curto da subscription ou nome completo `projects/.../subscriptions/...`);
   - opcionalmente `topic` em `pubsub` (apenas para logs/documentação — o consumo é sempre pela subscription).

   O código remove o bloco `pubsub` antes de construir as credenciais OAuth, pois não faz parte do formato oficial do Google.

3. **Subscriber** — `PubSubSubscriberConfig` cria um `Subscriber` do cliente Java do Pub/Sub apontando para a subscription resolvida. Um `MessageReceiver` delega o payload a `PubSubMessageHandler` e:
   - em caso de sucesso: chama **`ack()`** — a mensagem sai do backlog **dessa** subscription;
   - em caso de exceção (por exemplo payload vazio): **`nack()`** — a mensagem pode ser reentregue conforme política do Pub/Sub.

4. **Processamento** — `PubSubMessageHandler` valida que o corpo não esta vazio, registra metadados em log (subscription, topic, `messageId`, tamanho em bytes) **sem** logar o corpo bruto (evita vazar dados sensiveis), e em seguida deserializa o JSON para `OrderPayloadDto` e chama `OrderPersistenceService.persist()` para gravar no banco.

5. **Ciclo de vida** — `PubSubSubscriberLifecycle` inicia o subscriber no `@PostConstruct` e encerra com timeout no `@PreDestroy`.

6. **Processo em foreground** — Como não há servidor web, o `main` mantém a JVM ativa com um loop enquanto o contexto Spring estiver ativo, evitando que o processo termine quando só restarem threads daemon (por exemplo do gRPC do Pub/Sub).

### Como executar o backend

Na pasta `backend-v1`, com o JSON de credenciais no caminho configurado e (se necessário) o Postgres no ar:

```bash
cd backend-v1
./mvnw spring-boot:run
```

Ou, após `./mvnw package`:

```bash
java -jar target/messaging-consumer-0.0.1-SNAPSHOT.jar
```

### Desativar Pub/Sub localmente

Defina `app.pubsub.enabled=false` (por exemplo em `application.properties` ou variável de ambiente equivalente do Spring) para subir a aplicação sem conectar ao GCP — lembre-se de que o arquivo de credenciais e a subscription ainda podem ser exigidos em outros perfis conforme a configuração.

---

## Como os dados sao inseridos no banco

Cada mensagem recebida pelo Pub/Sub contem um JSON de pedido. O `PubSubMessageHandler` deserializa o payload para DTOs e delega ao `OrderPersistenceService`, que persiste os dados numa unica transacao (`@Transactional`), respeitando a ordem de dependencias de FK:

```text
GCP Pub/Sub (topic) → subscription → Subscriber (backend-v1)
                                          |
                              PubSubMessageHandler
                                   |          |
                            deserializa    ack/nack
                            JSON → DTO
                                   |
                        OrderPersistenceService.persist()
                                   |
               +-------------------+-------------------+
               |         |         |         |         |
           Category  SubCategory Customer  Seller   Product
               |         |         |         |         |
               +---------+---------+---------+---------+
                                   |
                                Orders
                                   |
                    +--------------+--------------+
                    |              |              |
                Shipment       Payment      OrderItem
```

### Ordem de persistencia

1. **Category** — upsert por ID (PK fornecida no payload)
2. **SubCategory** — upsert por ID, referenciando a Category salva
3. **Customer** — busca por `document` (CPF); se nao existe, cria novo (ID auto-gerado)
4. **Seller** — upsert por ID (PK fornecida no payload)
5. **Product** — busca por `productId` externo; se nao existe, cria novo (ID auto-gerado)
6. **Orders** — cria com `uuid` do payload, recalcula `total` somando `unitPrice * quantity` de cada item, e mapeia status desconhecidos para `created`
7. **Shipment** — cria vinculado ao pedido
8. **Payment** — cria vinculado ao pedido
9. **OrderItem** — cria vinculado ao pedido e produto; o campo `total` e calculado automaticamente pelo banco (`GENERATED ALWAYS AS`)

### Tabelas do banco

As tabelas sao criadas pelo script [`database/init/tables.sql`](database/init/tables.sql) na primeira inicializacao do container. O Hibernate roda com `ddl-auto=none`, ou seja, **nao altera o schema** — toda a estrutura e gerenciada pelo SQL de inicializacao.

| Tabela | PK | Descricao |
|--------|----|-----------|
| `customer` | `id` (SERIAL) | Clientes, identificados por `document` (CPF) |
| `seller` | `id` (INTEGER) | Vendedores |
| `category` | `id` (VARCHAR) | Categorias de produto |
| `sub_category` | `id` (VARCHAR) | Subcategorias, FK para `category` |
| `product` | `id` (SERIAL) | Produtos, com `product_id` externo unico |
| `orders` | `uuid` (VARCHAR) | Pedidos |
| `shipment` | `id` (SERIAL) | Envio, 1:1 com `orders` |
| `payment` | `id` (SERIAL) | Pagamento, 1:1 com `orders` |
| `order_item` | `id` (SERIAL) | Itens do pedido, com `total` gerado pelo banco |

---

## Resumo do fluxo

```text
GCP Pub/Sub (topic) → subscription → Subscriber (backend-v1)
                                          |
                              PubSubMessageHandler (ack/nack)
                                          |
                        OrderPersistenceService (@Transactional)
                                          |
                                     PostgreSQL
```

Para duvidas sobre permissoes no GCP, a service account usada no JSON precisa de permissao para consumir a subscription (por exemplo `roles/pubsub.subscriber` no topico/projeto conforme a politica da equipe).
