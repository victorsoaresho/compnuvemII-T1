# Arquitetura — messaging-consumer

## Visão Geral

O projeto é um **consumer assíncrono** de mensagens, sem API REST. O fluxo é unidirecional:

```
Google Cloud Pub/Sub
        │
        ▼
[PubSubSubscriberConfig]   ← configura e inicia o subscriber
        │  recebe PubsubMessage, chama ack() ou nack()
        ▼
[PubSubMessageHandler]     ← deserializa JSON → OrderPayloadDto
        │
        ▼
[OrderPersistenceService]  ← persiste tudo num @Transactional
        │
        ▼
[Repositories (JPA)]       ← upserts no PostgreSQL
```

---

## Camadas internas

| Pacote | Responsabilidade |
|--------|-----------------|
| `pubsub/` | Infraestrutura de mensageria: conexão com GCP, recebimento, ack/nack |
| `model/dto/` | Contrato de entrada — representa o JSON que vem do tópico |
| `model/` | Entidades JPA — representa o schema do banco |
| `service/` | Lógica de negócio: upserts, cálculo de total, mapeamento DTO → entidade |
| `repository/` | Acesso ao banco via Spring Data JPA + queries nativas de upsert |

---

## Decisões arquiteturais notáveis

1. **Sem controller/REST**: o ponto de entrada é o subscriber Pub/Sub, não HTTP.
2. **`@Transactional` na `persist()`**: garante que um pedido seja salvo por inteiro ou não salvo — sem dados parciais no banco.
3. **Upsert ao invés de insert**: evita duplicatas e race conditions quando a mesma mensagem é reentregue (comportamento padrão do Pub/Sub — *at-least-once delivery*).
4. **`TreeMap` para ordenar locks**: categorias são upsertadas em ordem de ID para evitar deadlocks entre threads concorrentes.
5. **`ack`/`nack` explícito**: se o processamento falhar, a mensagem volta para o backlog da subscription e será reentregue.
6. **`@ConditionalOnProperty`**: o subscriber só sobe se `app.pubsub.enabled=true`, facilitando testes sem precisar de conexão com GCP.

---

## Stack tecnológica

- **Spring Boot 3.5** + **Spring Data JPA**
- **Google Cloud Pub/Sub** (`spring-cloud-gcp-starter-pubsub`)
- **PostgreSQL** em produção / **H2** em testes
- **Jackson** para deserialização do payload JSON

---

## Fluxo de inicialização

### Passo 1 — `main()` é chamado

`MessagingConsumerApplication.java`

```java
SpringApplication.run(MessagingConsumerApplication.class, args);
```

O Spring Boot começa a montar o contexto. Como não há `spring-boot-starter-web` no `pom.xml`, ele sobe no modo **non-web** — sem Tomcat, sem porta HTTP.

---

### Passo 2 — Propriedades são carregadas

`PubSubProperties.java`

O Spring lê o `application.properties` e mapeia tudo com prefixo `app.pubsub` para essa classe:

```
app.pubsub.enabled=true
app.pubsub.credentials-location=file:./credentials/credentials.json
```

Os campos `projectId`, `subscriptionName` e `topicName` ainda estão **vazios** aqui — serão preenchidos no próximo passo.

---

### Passo 3 — Credenciais são lidas do arquivo JSON

`PubSubCredentialsConfiguration.java` → `PubSubCredentialsLoadResult.java`

O arquivo `credentials/credentials.json` é lido e o código faz:

1. Lê o JSON inteiro com Jackson
2. **Remove o bloco `pubsub`** do JSON — ele é customizado e não faz parte do formato oficial do Google
3. Extrai `project_id`, `subscription` e `topic` desse bloco
4. **Preenche os campos vazios** de `PubSubProperties` (projectId, subscriptionName, topicName)
5. Monta o `ProjectSubscriptionName` — o nome completo `projects/X/subscriptions/Y`
6. Cria o `GoogleCredentials` a partir do JSON **sem o bloco pubsub** (se deixasse, o Google rejeitaria)
7. Retorna tudo encapsulado num `PubSubCredentialsLoadResult`

---

### Passo 4 — Subscriber é construído

`PubSubSubscriberConfig.java`

Com as credenciais prontas, o Spring monta o bean `Subscriber` do GCP:

```java
Subscriber.newBuilder(subscriptionName, receiver)
    .setCredentialsProvider(credentialsProvider)
    .build();
```

A lambda `receiver` já está definida aqui mas ainda **não está escutando nada** — o subscriber está construído mas parado.

---

### Passo 5 — Subscriber é iniciado (`@PostConstruct`)

```java
subscriber.startAsync().awaitRunning();
log.info("pubsub_subscriber_started");
```

Aqui o subscriber **de fato conecta no GCP** e começa a escutar mensagens. A partir desse momento, qualquer mensagem publicada no tópico e endereçada a essa subscription será entregue.

---

### Passo 6 — `main()` entra no loop de espera

```java
while (ctx.isActive()) {
    Thread.sleep(500);
}
```

A aplicação está **viva e escutando**. A thread `main` fica em loop pois sem ela a JVM encerraria (as threads do gRPC do Pub/Sub são daemon).

---

### Passo 7 — Mensagem chega

Quando o GCP entrega uma mensagem, a lambda do `receiver` é chamada:

```
PubsubMessage → PubSubMessageHandler.handle()
    → Jackson desserializa JSON → OrderPayloadDto
    → OrderPersistenceService.persist()
        → upserts em Category, SubCategory, Customer, Seller, Product
        → inserts em Orders, Shipment, Payment, OrderItems
    → consumer.ack()   ✅ tudo certo
```

Se qualquer passo lançar exceção → `consumer.nack()` → mensagem volta para reentrega.

---

### Fluxo completo visual

```
JVM inicia
  └─ SpringApplication.run()
       ├─ PubSubProperties ← application.properties
       ├─ PubSubCredentialsLoadResult ← credentials.json
       │     ├─ extrai project_id, subscription, topic
       │     ├─ remove bloco "pubsub" do JSON
       │     └─ cria GoogleCredentials
       ├─ Subscriber.build() ← credenciais + receiver lambda
       └─ @PostConstruct → subscriber.startAsync()  ← COMEÇA A ESCUTAR

main() → while(ctx.isActive()) loop  ← mantém JVM viva

  GCP entrega mensagem
    └─ receiver lambda
         ├─ handle() → persist() → ack()   ✅
         └─ exception → nack()             ❌ reentrega

SIGTERM
  └─ @PreDestroy → subscriber.stopAsync() (timeout 30s)
  └─ ctx encerra → while sai → JVM termina
```

---

## Quebra do payload nas entidades

### O payload que chega

`OrderPayloadDto` representa o JSON completo:

```json
{
  "uuid": "...",
  "created_at": "...",
  "channel": "...",
  "status": "...",
  "customer": { ... },
  "seller": { ... },
  "items": [ { "category": { "subCategory": { ... } }, ... } ],
  "shipment": { ... },
  "payment": { ... },
  "metadata": { ... }
}
```

---

### Etapa 1 — Category e SubCategory

Itera sobre **todos os itens** do pedido e deduplica categorias por ID num `TreeMap`. A ordem garantida pelo `TreeMap` é proposital — evita deadlock entre threads que tentam upsert na mesma linha do banco em ordens diferentes.

Depois faz upsert de cada `Category` e `SubCategory` no banco.

---

### Etapa 2 — Customer

```
CustomerDto → upsertIfAbsent(nome, email, documento)
           → findByDocument() para pegar a entidade com ID gerado pelo banco
```

Usa o **documento** como chave de identidade — se o cliente já existe, não duplica. Depois busca o objeto de volta para ter o `id` gerado pelo banco e usar nas relações.

---

### Etapa 3 — Seller

```
SellerDto → new Seller(id, name, city, state)
          → sellerRepository.save()
```

O `id` do seller vem do próprio payload (não é gerado pelo banco), então um simples `save()` já faz upsert via JPA.

---

### Etapa 4 — Total calculado

```java
computedTotal += item.unitPrice * item.quantity  // para cada item
```

O total do payload é **ignorado** — o sistema recalcula a partir dos itens para garantir consistência. Evita aceitar um total manipulado na mensagem.

---

### Etapa 5 — Orders

```
OrderPayloadDto → Orders
  uuid, createdAt, channel, status (enum), total (calculado)
  metadata → source, userAgent, ipAddress
  customer (objeto JPA já carregado)
  seller   (objeto JPA já salvo)
```

O `indexedAt` é preenchido com `OffsetDateTime.now()` — marca quando **o sistema processou**, não quando o pedido foi criado.

---

### Etapa 6 — Shipment

```
ShipmentDto → Shipment
  carrier, service, status, trackingCode
  order → referência para o Orders recém-salvo
```

Opcional — só persiste se `dto.getShipment() != null`.

---

### Etapa 7 — Payment

```
PaymentDto → Payment
  method, status, transactionId
  order → referência para o Orders recém-salvo
```

Também opcional.

---

### Etapa 8 — Products e OrderItems

Para **cada item** do payload:

```
ItemDto → upsertIfAbsent(productId, productName, subCategoryId, unitPrice)
       → findByProductId() para pegar entidade com ID do banco

ItemDto + Product → OrderItem
  productName, unitPrice, quantity
  order   → referência para Orders
  product → referência para Product
  total   → GERADO PELO BANCO (coluna calculada, não setada aqui)
```

---

### Diagrama do mapeamento

```
OrderPayloadDto
├── customer     ──────────────────────────► Customer
├── seller       ──────────────────────────► Seller
├── (uuid, channel, status, metadata) ────► Orders ◄── Customer, Seller
├── shipment     ──────────────────────────► Shipment ◄── Orders
├── payment      ──────────────────────────► Payment  ◄── Orders
└── items[]
      ├── category      ─────────────────► Category
      ├── category.subCategory ─────────► SubCategory ◄── Category
      ├── (productId, productName...) ──► Product ◄── SubCategory
      └── (qty, price) ─────────────────► OrderItem ◄── Orders, Product
```

Tudo dentro de um único `@Transactional` — ou persiste tudo junto, ou não persiste nada.

---

## Referências

| Tecnologia | Documentação |
|------------|-------------|
| Spring Boot 3.x | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| Spring Data JPA | https://docs.spring.io/spring-data/jpa/reference/ |
| Google Cloud Pub/Sub | https://cloud.google.com/pubsub/docs |
| Spring Cloud GCP (Pub/Sub) | https://googlecloudplatform.github.io/spring-cloud-gcp/reference/html/index.html#spring-pubsub |
| PostgreSQL | https://www.postgresql.org/docs/ |
| H2 Database | https://h2database.com/html/main.html |
| Jackson (FasterXML) | https://fasterxml.github.io/jackson-databind/javadoc/2.x/ |
