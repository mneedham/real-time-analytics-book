import random, time
from mysql.connector import connect, Error
from faker import Faker
from faker.providers import company
import json
from kafka import KafkaProducer
import datetime
import uuid

# CONFIG
usersLimit         = 1000
orderInterval      = 100
mysqlHost          = 'mysql'
mysqlPort          = '3306'
mysqlUser          = 'mysqluser'
mysqlPass          = 'mysqlpw'
debeziumHostPort   = 'debezium:8083'
kafkaHostPort      = 'kafka:9092'

# INSERT TEMPLATES
insert_pizzashop_user_tpl    = "INSERT INTO pizzashop.users (first_name, last_name, email, country) VALUES ( %s, %s, %s, %s )"
insert_pizza_product_tpl     = "INSERT INTO pizzashop.products (name, category, price) VALUES ( %s, %s, %s )"

fake = Faker()
fake.add_provider(company)

with open("data/products.json", "r") as products_file:
    products = [json.loads(row) for row in products_file.readlines()]

producer = KafkaProducer(bootstrap_servers=kafkaHostPort, api_version=(7, 1, 0), 
  value_serializer=lambda m: json.dumps(m).encode('utf-8'))


events_processed = 0
try:
    with connect(
        host=mysqlHost,
        user=mysqlUser,
        password=mysqlPass,
    ) as connection:
        with connection.cursor() as cursor:
            print("Seeding pizzashop database...")
            cursor.execute("TRUNCATE TABLE pizzashop.users")
            cursor.execute("TRUNCATE TABLE pizzashop.products")

            cursor.executemany(
                insert_pizzashop_user_tpl,
                [
                    (
                        fake.first_name(),
                        fake.last_name(),
                        fake.email(),
                        fake.country()
                     ) for i in range(usersLimit)
                ]
            )
            cursor.executemany(
                insert_pizza_product_tpl,
                [
                    (
                        product["name"],
                        product["category"],
                        product["price"]
                    ) for product in products
                ]
            )
            connection.commit()

            print("Getting product ID and PRICE as tuples...")
            cursor.execute("SELECT id, price FROM pizzashop.products")
            product_prices = [(row[0], row[1]) for row in cursor]
            print(product_prices)

            while True:
                # Get a random a user and a product to order
                product = random.choice(product_prices)
                user = random.randint(0,usersLimit-1)
                purchase_quantity = random.randint(1,5)

                event = {
                    "id": str(uuid.uuid4()),
                    "createdAt": datetime.datetime.now().isoformat(),
                    "userId": user,
                    "productId": product[0],
                    "quantity": purchase_quantity,
                    "total": product[1] * purchase_quantity,
                    "status": "PLACED_ORDER"
                }

                producer.send('orders', event)

                events_processed += 1
                if events_processed % 100 == 0:
                    print(f"{str(datetime.datetime.now())} Flushing after {events_processed} events")
                    producer.flush()

                time.sleep(random.randint(orderInterval/5, orderInterval)/1000)

    connection.close()

except Error as e:
    print(e)