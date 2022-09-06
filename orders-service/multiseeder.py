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
mysqlHost          = 'localhost'
mysqlPort          = '3306'
mysqlUser          = 'mysqluser'
mysqlPass          = 'mysqlpw'
debeziumHostPort   = 'debezium:8083'
kafkaHostPort      = 'localhost:29092'

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
            print("Getting products for the products topic")
            cursor.execute("SELECT id, name, description, category, price, image FROM pizzashop.products")
            products = [{
                "id": str(row[0]),
                "name": row[1],
                "description": row[2],
                "category": row[3],
                "price": row[4],
                "image": row[5]
                }
                for row in cursor
            ]

            for product in products:
                print(product["id"])
                producer.send('products-multi2', product, product["id"].encode("UTF-8"))
            producer.flush()

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
                    "status": "PLACED_ORDER",

                    "items": [
                        { 
                            "productId": str(product[0]),
                            "quantity": purchase_quantity,
                            "price": product[1]
                        }
                    ]
                }

                producer.send('orders-multi2', event, bytes(event["id"].encode("UTF-8")))

                events_processed += 1
                if events_processed % 100 == 0:
                    print(f"{str(datetime.datetime.now())} Flushing after {events_processed} events")
                    producer.flush()

                time.sleep(random.randint(orderInterval/5, orderInterval)/1000)

    connection.close()

except Error as e:
    print(e)