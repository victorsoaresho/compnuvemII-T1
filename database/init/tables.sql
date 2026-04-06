-- =============================================================
-- Projeto Mensageria - Computação em Nuvem 2 - FATEC
-- Criação inicial das tabelas
-- =============================================================

-- -------------------------------------------------------------
-- CUSTOMER
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer (
    id       SERIAL       PRIMARY KEY,
    name     VARCHAR(150) NOT NULL,
    email    VARCHAR(150) NOT NULL,
    document VARCHAR(20)  NOT NULL UNIQUE,
    CONSTRAINT uq_customer_id UNIQUE (id)
);

-- -------------------------------------------------------------
-- SELLER
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seller (
    id    INTEGER      PRIMARY KEY,
    name  VARCHAR(150) NOT NULL,
    city  VARCHAR(100),
    state CHAR(2)
);

-- -------------------------------------------------------------
-- CATEGORY
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
    id   VARCHAR(20)  PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- -------------------------------------------------------------
-- SUB_CATEGORY
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sub_category (
    id          VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category_id VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_sub_category_category FOREIGN KEY (category_id)
        REFERENCES category (id)
);

-- -------------------------------------------------------------
-- PRODUCT
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id              SERIAL         PRIMARY KEY,
    product_id      INTEGER        NOT NULL UNIQUE,
    product_name    VARCHAR(200)   NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL,
    sub_category_id VARCHAR(20),
    CONSTRAINT fk_product_sub_category FOREIGN KEY (sub_category_id)
        REFERENCES sub_category (id)
);

-- -------------------------------------------------------------
-- ORDERS
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    uuid        VARCHAR(50)    PRIMARY KEY,
    created_at  TIMESTAMPTZ    NOT NULL,
    indexed_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    channel     VARCHAR(50),
    total       NUMERIC(12, 2),
    status      VARCHAR(20)    NOT NULL
                    CHECK (status IN ('created', 'paid', 'shipped', 'delivered', 'canceled')),
    customer_id INTEGER        NOT NULL,
    seller_id   INTEGER        NOT NULL,
    -- metadata (embutida conforme definição do projeto)
    source      VARCHAR(50),
    user_agent  TEXT,
    ip_address  VARCHAR(45),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customer (id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id)
        REFERENCES seller (id)
);

-- -------------------------------------------------------------
-- SHIPMENT
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shipment (
    id            SERIAL       PRIMARY KEY,
    order_uuid    VARCHAR(50)  NOT NULL UNIQUE,
    carrier       VARCHAR(100),
    service       VARCHAR(50),
    status        VARCHAR(50),
    tracking_code VARCHAR(100),
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_uuid)
        REFERENCES orders (uuid)
);

-- -------------------------------------------------------------
-- PAYMENT
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment (
    id             SERIAL       PRIMARY KEY,
    order_uuid     VARCHAR(50)  NOT NULL UNIQUE,
    method         VARCHAR(50),
    status         VARCHAR(50),
    transaction_id VARCHAR(100),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_uuid)
        REFERENCES orders (uuid)
);

-- -------------------------------------------------------------
-- ORDER_ITEM
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_item (
    id           SERIAL         PRIMARY KEY,
    order_uuid   VARCHAR(50)    NOT NULL,
    product_id   INTEGER        NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    total        NUMERIC(12, 2) GENERATED ALWAYS AS (unit_price * quantity) STORED,
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_uuid) REFERENCES orders (uuid),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);

-- -------------------------------------------------------------
-- INDEXES
-- -------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_orders_customer_id  ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_seller_id    ON orders (seller_id);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at   ON orders (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_item_order    ON order_item (order_uuid);
CREATE INDEX IF NOT EXISTS idx_order_item_product  ON order_item (product_id);
CREATE INDEX IF NOT EXISTS idx_product_product_id  ON product (product_id);