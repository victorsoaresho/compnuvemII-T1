# Spec: Consumo de mensagens Pub/Sub → PostgreSQL

## Objetivo

Estender o `backend-v1` para que, ao receber uma mensagem do Google Cloud Pub/Sub:
1. O payload JSON seja parseado nos DTOs do pacote `model.dto`
2. Os dados sejam persistidos nas tabelas do banco usando as entidades já definidas no pacote `model`
3. O `ack()` seja confirmado automaticamente após persistência com sucesso (comportamento já existe em `PubSubSubscriberConfig`)

---

## Entidades existentes (vindas do zip `models/compnuvemII-T1/backend-v1.zip`)

Pacote: `br.com.fatec.messaging.model`

| Classe | Tabela | PK | Observação |
|---|---|---|---|
| `Category` | `category` | `String id` | PK é o id do payload (`"ELEC"`) |
| `SubCategory` | `sub_category` | `String id` | ManyToOne → `Category` |
| `Customer` | `customer` | `Long id` (SERIAL) | Identificar por `document` (UNIQUE) |
| `Seller` | `seller` | `Integer id` | PK vem diretamente do payload |
| `Product` | `product` | `Long id` (SERIAL) | Campo `productId` (UNIQUE) recebe o id do payload |
| `Orders` | `orders` | `String uuid` | Embute metadata (`source`, `userAgent`, `ipAddress`); usa `OrderStatus` enum |
| `OrderStatus` | — | enum | `created, paid, shipped, delivered, canceled` |
| `Shipment` | `shipment` | `Long id` (SERIAL) | OneToOne → `Orders` |
| `Payment` | `payment` | `Long id` (SERIAL) | OneToOne → `Orders` |
| `OrderItem` | `order_item` | `Long id` (SERIAL) | ManyToOne → `Orders` e `Product`; `total` com `insertable=false, updatable=false` (GENERATED) |

---

## Repositories existentes (vindos do zip)

Pacote: `br.com.fatec.messaging.repository`

| Interface | Tipo PK | Precisa de método extra? |
|---|---|---|
| `CategoryRepository` | `String` | Não |
| `CustomerRepository` | `Long` | **Sim** — `findByDocument(String)` para upsert |
| `SubCategoryRepository` | `String` | Não |
| `SellerRepository` | `Integer` | Não |
| `ProductRepository` | `Long` | **Sim** — `findByProductId(Integer)` para upsert |
| `OrdersRepository` | `String` | Não |
| `ShipmentRepository` | `Long` | Não |
| `PaymentRepository` | `Long` | Não |
| `OrderItemRepository` | `Long` | Não |

---

## Observações sobre o payload vs. esquema do banco

| Campo payload | Entidade / coluna | Observação |
|---|---|---|
| `customer.id` | `Customer.id` (SERIAL) | PK gerada pelo banco. Upsert via `findByDocument(document)`. |
| `seller.id` | `Seller.id` (Integer PK) | PK vem direto do payload. Usar `save()` com id informado. |
| `items[].product_id` | `Product.productId` (UNIQUE) | `Product.id` é SERIAL. Upsert via `findByProductId(productId)`. |
| `status: "separated"` | `Orders.status` (`OrderStatus` enum) | `"separated"` não existe no enum. **Mapear para `OrderStatus.created`** no service. |
| `items[].total` | `OrderItem.total` | `insertable=false, updatable=false` — banco calcula via `GENERATED`. Não atribuir. |
| `order.total` | `Orders.total` | Calcular no service: `sum(unitPrice * quantity)` dos itens. |
| `metadata` | `Orders (source, userAgent, ipAddress)` | Embutido diretamente na entidade `Orders`. |
| `created_at` | `Orders.createdAt` (`LocalDateTime`) | Payload tem timezone (`Z`). Parsear como `OffsetDateTime` e converter para `LocalDateTime` UTC. |

---

## Arquivos a CRIAR

### DTOs — parse do payload JSON
Pacote: `br.com.fatec.messaging.model.dto`

| # | Arquivo | Campos principais |
|---|---|---|
| 1 | `model/dto/OrderPayloadDto.java` | `uuid`, `createdAt` (String), `channel`, `total`, `status`, `customer`, `seller`, `items`, `shipment`, `payment`, `metadata` |
| 2 | `model/dto/CustomerDto.java` | `id`, `name`, `email`, `document` |
| 3 | `model/dto/SellerDto.java` | `id`, `name`, `city`, `state` |
| 4 | `model/dto/ItemDto.java` | `id`, `productId`, `productName`, `unitPrice`, `quantity`, `category`, `total` |
| 5 | `model/dto/CategoryDto.java` | `id`, `name`, `subCategory` |
| 6 | `model/dto/SubCategoryDto.java` | `id`, `name` |
| 7 | `model/dto/ShipmentDto.java` | `carrier`, `service`, `status`, `trackingCode` |
| 8 | `model/dto/PaymentDto.java` | `method`, `status`, `transactionId` |
| 9 | `model/dto/MetadataDto.java` | `source`, `userAgent`, `ipAddress` |

### Service — orquestração da persistência
Pacote: `br.com.fatec.messaging.service`

| # | Arquivo | Responsabilidade |
|---|---|---|
| 10 | `service/OrderPersistenceService.java` | Recebe `OrderPayloadDto`, executa a sequência de upserts/inserts em `@Transactional`. Normaliza `status`. Converte `createdAt` para `LocalDateTime`. Calcula `total` da ordem. |

**Total: 10 arquivos a criar**

---

## Arquivos a EDITAR

| # | Arquivo | O que muda |
|---|---|---|
| 1 | `repository/CustomerRepository.java` | Adicionar `Optional<Customer> findByDocument(String document)` |
| 2 | `repository/ProductRepository.java` | Adicionar `Optional<Product> findByProductId(Integer productId)` |
| 3 | `pubsub/PubSubMessageHandler.java` | Injetar `ObjectMapper` e `OrderPersistenceService`. No `handle()`: deserializar `data` para `OrderPayloadDto` e chamar `orderPersistenceService.persist(dto)`. |

**Total: 3 arquivos a editar**

---

## Fluxo de execução (sequência)

```
Pub/Sub → PubSubSubscriberConfig (receiver)
            └─ PubSubMessageHandler.handle(message)
                  ├─ ObjectMapper.readValue(data, OrderPayloadDto.class)
                  └─ OrderPersistenceService.persist(dto)
                        ├─ upsert Category          (save por id String)
                        ├─ upsert SubCategory       (save por id String, ref Category)
                        ├─ upsert Customer          (findByDocument → save)
                        ├─ upsert Seller            (save por id Integer)
                        ├─ upsert Product           (findByProductId → save, ref SubCategory)
                        ├─ insert Orders            (uuid PK, ref Customer + Seller, metadata, status normalizado)
                        ├─ insert Shipment          (ref Orders)
                        ├─ insert Payment           (ref Orders)
                        └─ insert OrderItem[]       (ref Orders + Product, sem total)
            └─ consumer.ack()   ← só chega aqui se não houver exceção
```

---

## Estrutura de diretórios final

```
backend-v1/src/main/java/br/com/fatec/messaging/
├── MessagingConsumerApplication.java
├── model/
│   ├── Category.java               [EXISTENTE - do zip]
│   ├── SubCategory.java            [EXISTENTE - do zip]
│   ├── Customer.java               [EXISTENTE - do zip]
│   ├── Seller.java                 [EXISTENTE - do zip]
│   ├── Product.java                [EXISTENTE - do zip]
│   ├── Orders.java                 [EXISTENTE - do zip]
│   ├── OrderStatus.java            [EXISTENTE - do zip]
│   ├── Shipment.java               [EXISTENTE - do zip]
│   ├── Payment.java                [EXISTENTE - do zip]
│   ├── OrderItem.java              [EXISTENTE - do zip]
│   └── dto/
│       ├── OrderPayloadDto.java    [CRIAR]
│       ├── CustomerDto.java        [CRIAR]
│       ├── SellerDto.java          [CRIAR]
│       ├── ItemDto.java            [CRIAR]
│       ├── CategoryDto.java        [CRIAR]
│       ├── SubCategoryDto.java     [CRIAR]
│       ├── ShipmentDto.java        [CRIAR]
│       ├── PaymentDto.java         [CRIAR]
│       └── MetadataDto.java        [CRIAR]
├── repository/
│   ├── CategoryRepository.java     [EXISTENTE - do zip]
│   ├── SubCategoryRepository.java  [EXISTENTE - do zip]
│   ├── CustomerRepository.java     [EDITAR — adicionar findByDocument]
│   ├── SellerRepository.java       [EXISTENTE - do zip]
│   ├── ProductRepository.java      [EDITAR — adicionar findByProductId]
│   ├── OrdersRepository.java       [EXISTENTE - do zip]
│   ├── ShipmentRepository.java     [EXISTENTE - do zip]
│   ├── PaymentRepository.java      [EXISTENTE - do zip]
│   └── OrderItemRepository.java    [EXISTENTE - do zip]
├── service/
│   └── OrderPersistenceService.java [CRIAR]
└── pubsub/
    ├── PubSubCredentialsConfiguration.java
    ├── PubSubCredentialsLoadResult.java
    ├── PubSubMessageHandler.java   [EDITAR]
    ├── PubSubProperties.java
    └── PubSubSubscriberConfig.java
```

---

## Resumo

| Ação | Quantidade |
|---|---|
| Criar | 10 arquivos (9 DTOs + 1 service) |
| Editar | 3 arquivos (2 repositories + 1 handler) |
| Reaproveitar do zip | 10 entidades + 9 repositories já prontos |
