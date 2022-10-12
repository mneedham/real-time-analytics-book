import random, time
from mysql.connector import connect, Error
from faker import Faker
from faker.providers import company
import json
from kafka import KafkaProducer
import datetime
import uuid
import math
import os
from dateutil import parser
from sortedcontainers import SortedList

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

            cursor.execute("SELECT id FROM pizzashop.users")
            users = [row[0] for row in cursor]

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
        user = random.choice(users)
        purchase_quantity = random.randint(1,5)
        items.append({
            "productId": str(product[0]),
            "quantity": purchase_quantity,
            "price": product[1]
        })

    prices = [item["quantity"] * item["price"] for item in items]
    total_price = round(math.fsum(prices), 2)

    return {
        "id": str(uuid.uuid4()),
        "createdAt": datetime.datetime.now().isoformat(),
        "userId": user,
        "status": "PLACED_ORDER",
        "price": total_price,
        "items": items              
    }

STATUSES = [
    "PLACED_ORDER",
    "ORDER_CONFIRMED",
    "BEING_PREPARED",
    "OUT_FOR_DELIVERY",
    "ARRIVING_AT_DOOR",
    "DELIVERED"
]

WAIT_RANGES = {
    "ORDER_CONFIRMED": (5, 20),
    "BEING_PREPARED": (5, 20),
    "OUT_FOR_DELIVERY": (5, 20),
    "ARRIVING_AT_DOOR": (5, 20),
    "DELIVERED": (5, 20)
}

other_statuses = SortedList(key=lambda x: x["updatedAt"])

while True:
    order = create_new_order()

    order_status = {
        "id": order["id"],
        "updatedAt": order["createdAt"],
        "status": "PLACED_ORDER"
    }

    producer.send('orders', order, bytes(order["id"].encode("UTF-8")))
    producer.send('ordersStatuses', order_status, bytes(order_status["id"].encode("UTF-8")))

    # generate statuses for this order
    placed_order_time = parser.parse(order["createdAt"])
    
    # look into random.choices for future
    # random.choices(range(0,100), range(0,100), k=100)

    # process other statuses
    for index in range(1, len(STATUSES)):
        last_status_time = placed_order_time
        status = STATUSES[index]
        min, max = WAIT_RANGES[status]
        next_status_time = last_status_time + datetime.timedelta(seconds=random.randint(min, max))

        other_statuses.add({
            "id": order["id"],
            "updatedAt": next_status_time.isoformat(),
            "status": status
        })
        last_status_time = next_status_time

    other_statuses_to_publish = [item for item in other_statuses if parser.parse(item["updatedAt"]) < datetime.datetime.now()]

    for status in other_statuses_to_publish:
        producer.send('ordersStatuses', status, bytes(status["id"].encode("UTF-8")))
        other_statuses.remove(status)        

    events_processed += 1
    events_processed + len(other_statuses_to_publish)
    if events_processed % 100 == 0:
        print(f"{str(datetime.datetime.now())} Flushing after {events_processed} events")
        producer.flush()

    time.sleep(random.randint(orderInterval/5, orderInterval)/1000)

