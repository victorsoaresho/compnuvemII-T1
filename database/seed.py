"""
seed.py – Popula o banco compnuvem-t1 com dados fictícios.

Dependências:
    pip install psycopg2-binary faker

Uso:
    python seed.py                  # usa defaults (localhost / postgres / postgres)
    DB_HOST=10.0.0.5 python seed.py # override via env vars
"""

import os
import random
import uuid
from datetime import datetime, timedelta, timezone

import psycopg2
from faker import Faker

# ── Configuração ────────────────────────────────────────────────
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 5432)),
    "dbname": os.getenv("DB_NAME", "compnuvem-t1"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD", "postgres"),
}

SEED_CUSTOMERS = 50
SEED_SELLERS = 20
SEED_ORDERS = 200

fake = Faker("pt_BR")
Faker.seed(42)
random.seed(42)

# ── Dados fixos ─────────────────────────────────────────────────
CATEGORIES = [
    ("CAT01", "Eletrônicos"),
    ("CAT02", "Roupas"),
    ("CAT03", "Alimentos"),
    ("CAT04", "Livros"),
    ("CAT05", "Casa e Jardim"),
]

SUB_CATEGORIES = [
    ("SUB01", "Smartphones", "CAT01"),
    ("SUB02", "Notebooks", "CAT01"),
    ("SUB03", "Acessórios", "CAT01"),
    ("SUB04", "Camisetas", "CAT02"),
    ("SUB05", "Calças", "CAT02"),
    ("SUB06", "Tênis", "CAT02"),
    ("SUB07", "Bebidas", "CAT03"),
    ("SUB08", "Snacks", "CAT03"),
    ("SUB09", "Ficção", "CAT04"),
    ("SUB10", "Técnicos", "CAT04"),
    ("SUB11", "Móveis", "CAT05"),
    ("SUB12", "Ferramentas", "CAT05"),
]

PRODUCTS = [
    (1001, "iPhone 15 Pro", 7999.00, "SUB01"),
    (1002, "Samsung Galaxy S24", 5499.00, "SUB01"),
    (1003, "MacBook Air M3", 12999.00, "SUB02"),
    (1004, "Dell Inspiron 15", 4299.00, "SUB02"),
    (1005, "Fone Bluetooth JBL", 299.90, "SUB03"),
    (1006, "Carregador USB-C 65W", 149.90, "SUB03"),
    (1007, "Camiseta Básica Preta", 49.90, "SUB04"),
    (1008, "Camiseta Polo Branca", 89.90, "SUB04"),
    (1009, "Calça Jeans Slim", 159.90, "SUB05"),
    (1010, "Calça Moletom", 99.90, "SUB05"),
    (1011, "Tênis Nike Air Max", 699.90, "SUB06"),
    (1012, "Tênis Adidas Ultraboost", 899.90, "SUB06"),
    (1013, "Café Especial 500g", 45.90, "SUB07"),
    (1014, "Suco Integral 1L", 12.90, "SUB07"),
    (1015, "Mix de Castanhas 200g", 29.90, "SUB08"),
    (1016, "Barra de Cereal cx12", 18.90, "SUB08"),
    (1017, "O Senhor dos Anéis", 79.90, "SUB09"),
    (1018, "Duna", 59.90, "SUB09"),
    (1019, "Clean Code", 119.90, "SUB10"),
    (1020, "Design Patterns", 139.90, "SUB10"),
    (1021, "Mesa de Escritório", 599.90, "SUB11"),
    (1022, "Cadeira Gamer", 1299.90, "SUB11"),
    (1023, "Furadeira Bosch", 349.90, "SUB12"),
    (1024, "Jogo de Chaves", 89.90, "SUB12"),
    (1025, "Webcam Full HD", 199.90, "SUB03"),
]

STATES = ["SP", "RJ", "MG", "RS", "PR", "SC", "BA", "PE", "CE", "DF"]
CHANNELS = ["web", "mobile", "marketplace", "whatsapp"]
ORDER_STATUSES = ["created", "paid", "shipped", "delivered", "canceled"]
PAYMENT_METHODS = ["credit_card", "debit_card", "pix", "boleto"]
PAYMENT_STATUSES = ["approved", "pending", "rejected"]
CARRIERS = ["Correios", "Jadlog", "Loggi", "Azul Cargo", "Total Express"]
CARRIER_SERVICES = ["PAC", "SEDEX", "Expresso", "Econômico", "Same-day"]
SHIPMENT_STATUSES = ["preparing", "shipped", "in_transit", "delivered", "returned"]


def connect():
    print(f"Conectando em {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['dbname']} ...")
    return psycopg2.connect(**DB_CONFIG)


def truncate_all(cur):
    """Limpa todas as tabelas respeitando FKs."""
    cur.execute("""
        TRUNCATE order_item, shipment, payment, orders,
                 product, sub_category, category,
                 seller, customer
        CASCADE;
    """)
    print("Tabelas truncadas.")


def seed_customers(cur):
    rows = []
    for _ in range(SEED_CUSTOMERS):
        rows.append((
            fake.name(),
            fake.email(),
            fake.cpf(),
        ))
    cur.executemany(
        "INSERT INTO customer (name, email, document) VALUES (%s, %s, %s)",
        rows,
    )
    print(f"  ✓ {len(rows)} customers inseridos")


def seed_sellers(cur):
    rows = []
    for i in range(1, SEED_SELLERS + 1):
        rows.append((
            i,
            fake.company(),
            fake.city(),
            random.choice(STATES),
        ))
    cur.executemany(
        "INSERT INTO seller (id, name, city, state) VALUES (%s, %s, %s, %s)",
        rows,
    )
    print(f"  ✓ {len(rows)} sellers inseridos")


def seed_categories(cur):
    cur.executemany(
        "INSERT INTO category (id, name) VALUES (%s, %s)",
        CATEGORIES,
    )
    print(f"  ✓ {len(CATEGORIES)} categories inseridas")


def seed_sub_categories(cur):
    cur.executemany(
        "INSERT INTO sub_category (id, name, category_id) VALUES (%s, %s, %s)",
        SUB_CATEGORIES,
    )
    print(f"  ✓ {len(SUB_CATEGORIES)} sub_categories inseridas")


def seed_products(cur):
    cur.executemany(
        "INSERT INTO product (product_id, product_name, unit_price, sub_category_id) VALUES (%s, %s, %s, %s)",
        PRODUCTS,
    )
    # Busca os IDs gerados (SERIAL) pra usar nos order_items
    cur.execute("SELECT id, product_id FROM product")
    product_map = {row[1]: row[0] for row in cur.fetchall()}
    print(f"  ✓ {len(PRODUCTS)} products inseridos")
    return product_map


def seed_orders(cur, product_map):
    product_ids = list(product_map.keys())
    product_prices = {p[0]: p[2] for p in PRODUCTS}
    product_names = {p[0]: p[1] for p in PRODUCTS}

    orders_created = 0
    items_created = 0
    shipments_created = 0
    payments_created = 0

    for _ in range(SEED_ORDERS):
        order_uuid = str(uuid.uuid4())
        created_at = fake.date_time_between(
            start_date="-6M", end_date="now", tzinfo=timezone.utc
        )
        status = random.choice(ORDER_STATUSES)
        customer_id = random.randint(1, SEED_CUSTOMERS)
        seller_id = random.randint(1, SEED_SELLERS)
        channel = random.choice(CHANNELS)

        # Itens do pedido (1 a 5)
        num_items = random.randint(1, 5)
        chosen_products = random.sample(product_ids, min(num_items, len(product_ids)))
        order_total = 0.0
        items = []
        for pid in chosen_products:
            qty = random.randint(1, 3)
            price = float(product_prices[pid])
            order_total += price * qty
            items.append((
                order_uuid,
                product_map[pid],
                product_names[pid],
                price,
                qty,
            ))

        # Insert order
        cur.execute(
            """INSERT INTO orders (uuid, created_at, channel, total, status,
                                   customer_id, seller_id, source, user_agent, ip_address)
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
            (
                order_uuid,
                created_at,
                channel,
                round(order_total, 2),
                status,
                customer_id,
                seller_id,
                random.choice(["google", "direct", "instagram", "facebook", "tiktok"]),
                fake.user_agent(),
                fake.ipv4(),
            ),
        )
        orders_created += 1

        # Insert items
        cur.executemany(
            """INSERT INTO order_item (order_uuid, product_id, product_name, unit_price, quantity)
               VALUES (%s, %s, %s, %s, %s)""",
            items,
        )
        items_created += len(items)

        # Insert payment
        cur.execute(
            """INSERT INTO payment (order_uuid, method, status, transaction_id)
               VALUES (%s, %s, %s, %s)""",
            (
                order_uuid,
                random.choice(PAYMENT_METHODS),
                random.choice(PAYMENT_STATUSES),
                fake.uuid4(),
            ),
        )
        payments_created += 1

        # Insert shipment (80% das ordens têm shipment)
        if random.random() < 0.8:
            cur.execute(
                """INSERT INTO shipment (order_uuid, carrier, service, status, tracking_code)
                   VALUES (%s, %s, %s, %s, %s)""",
                (
                    order_uuid,
                    random.choice(CARRIERS),
                    random.choice(CARRIER_SERVICES),
                    random.choice(SHIPMENT_STATUSES),
                    fake.bothify("??#########BR").upper(),
                ),
            )
            shipments_created += 1

    print(f"  ✓ {orders_created} orders inseridas")
    print(f"  ✓ {items_created} order_items inseridos")
    print(f"  ✓ {payments_created} payments inseridos")
    print(f"  ✓ {shipments_created} shipments inseridos")


def main():
    conn = connect()
    try:
        with conn.cursor() as cur:
            print("\n🗑  Limpando tabelas...")
            truncate_all(cur)

            print("\n📦 Populando dados...")
            seed_customers(cur)
            seed_sellers(cur)
            seed_categories(cur)
            seed_sub_categories(cur)
            product_map = seed_products(cur)
            seed_orders(cur, product_map)

        conn.commit()
        print("\n✅ Seed concluído com sucesso!\n")
    except Exception as e:
        conn.rollback()
        print(f"\n❌ Erro: {e}\n")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
