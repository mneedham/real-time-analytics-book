import random, time
from mysql.connector import connect, Error
import json
from kafka import KafkaProducer
import datetime
import uuid
import math
import os


# CONFIG
usersLimit         = 1000
orderInterval      = 100
mysqlHost          = os.environ.get("MYSQL_SERVER", "localhost")
mysqlPort          = '3306'
mysqlUser          = 'mysqluser'
mysqlPass          = 'mysqlpw'
debeziumHostPort   = 'debezium:8083'
kafkaHostPort      = f"{os.environ.get('KAFKA_BROKER_HOSTNAME', 'localhost')}:{os.environ.get('KAFKA_BROKER_PORT', '29092')}"

print(f"Kafka broker: {kafkaHostPort}")

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
                producer.send('products', product, product["id"].encode("UTF-8"))
            producer.flush()

            cursor.execute("SELECT id,lat,lon FROM pizzashop.users")
            users = {row[0]: (row[1], row[2]) for row in cursor}
            user_ids = list(users.keys())

            print("Getting product ID and PRICE as tuples...")
            cursor.execute("SELECT id, price FROM pizzashop.products")
            product_prices = [(row[0], row[1]) for row in cursor]
            print(product_prices)

    connection.close()

except Error as e:
    print(e)

def create_new_order():
    number_of_items = random.randint(1,10)

    items = []
    for _ in range(0, number_of_items):
        product = random.choice(product_prices)        
        purchase_quantity = random.randint(1,5)
        items.append({
            "productId": str(product[0]),
            "quantity": purchase_quantity,
            "price": product[1]
        })

    user_id = random.choice(user_ids)
    prices = [item["quantity"] * item["price"] for item in items]
    total_price = round(math.fsum(prices), 2)

    return {
        "id": str(uuid.uuid4()),
        "createdAt": datetime.datetime.now().isoformat(),
        "userId": user_id,
        "price": total_price,
        "items": items,
        "deliveryLat": str(users[user_id][0]),
        "deliveryLon": str(users[user_id][1])
    }

while True:
    order = create_new_order()

    producer.send('orders', order, bytes(order["id"].encode("UTF-8")))

    events_processed += 1
    if events_processed % 100 == 0:
        print(f"{str(datetime.datetime.now())} Flushing after {events_processed} events")
        producer.flush()

    time.sleep(random.randint(orderInterval/5, orderInterval)/1000)

