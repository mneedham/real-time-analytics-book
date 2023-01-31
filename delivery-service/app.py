import faust
import datetime
import random
from dateutil import parser
from sortedcontainers import SortedList
from mysql.connector import connect, Error


from pyproj import Geod
import pyproj
import geopy.distance

import os

orderInterval      = 100
mysqlHost          = os.environ.get("MYSQL_SERVER", "localhost")
mysqlPort          = '3306'
mysqlUser          = 'mysqluser'
mysqlPass          = 'mysqlpw'


connection = connect(
    host=mysqlHost,
    user=mysqlUser,
    password=mysqlPass,
)

STATUSES = [
    # PLACED_ORDER
    "ORDER_CONFIRMED",
    "BEING_PREPARED",
    "BEING_COOKED",
    "OUT_FOR_DELIVERY"
    # DELIVERED
]

WAIT_RANGES = {
    "ORDER_CONFIRMED": (60, 300),
    "BEING_PREPARED": (30, 120),
    "BEING_COOKED": (120, 180),
    "OUT_FOR_DELIVERY": (180,600)
}

app = faust.App(
    'DeliveryService',
    broker='kafka://kafka:9092',
    value_serializer='json',
)

orders_topic = app.topic('orders')
orders_statuses_topic = app.topic('ordersStatuses')
delivery_statuses_topic = app.topic('deliveryStatuses')

other_statuses = SortedList(key=lambda x: x["updatedAt"])
delivery_statuses = SortedList(key=lambda x: x["updatedAt"])

def generate_delivery_statuses(order, last_status_time, shop_location, delivery_location, points_to_generate):
    statuses = []
    if points_to_generate > 0:
        try:
            extra_points = geoid.npts(shop_location[0], shop_location[1], delivery_location[0], delivery_location[1], points_to_generate)
            for point in extra_points:    
                next_status_time = last_status_time + datetime.timedelta(seconds=1)

                statuses.append({
                    "id": order["id"],
                    "updatedAt": next_status_time.isoformat(),
                    "deliveryLat": str(point[0]),
                    "deliveryLon": str(point[1]),
                    "status": "IN_TRANSIT"
                })
                last_status_time = next_status_time
        except pyproj.exceptions.GeodError as e:
            print(e)

    statuses.append({
        "id": order["id"],
        "updatedAt": (last_status_time + datetime.timedelta(seconds=1)).isoformat(),
        "deliveryLat": str(delivery_location[0]),
        "deliveryLon": str(delivery_location[1]),
        "status": "DELIVERED"
    })
    return statuses

shop_location = (12.978268132410502, 77.59408889388118)
driver_km_per_hour = 150
geoid = Geod(ellps="WGS84")

@app.agent(orders_topic)
async def orders(stream):
    async for order in stream:        
        order_status = {
            "id": order["id"],
            "updatedAt": order["createdAt"],
            "status": "PLACED_ORDER"
        }

        await orders_statuses_topic.send(
            key=order_status["id"],
            value=order_status
        )

        placed_order_time = parser.parse(order["createdAt"])
        last_status_time = placed_order_time
        for index in range(0, len(STATUSES)):
            status = STATUSES[index]
            # min, max = WAIT_RANGES[status]
            min, max = tuple([item/10 for item in WAIT_RANGES[status]])
            next_status_time = last_status_time + datetime.timedelta(seconds=random.randint(min, max))

            other_statuses.add({
                "id": order["id"],
                "updatedAt": next_status_time.isoformat(),
                "status": status
            })
            last_status_time = next_status_time

        cursor = connection.cursor()
        cursor.execute("SELECT lat,lon FROM pizzashop.users WHERE id = %s", (order["userId"],))        
        delivery_location = [(row[0], row[1]) for row in cursor][0]
        cursor.close()

        dist = geopy.distance.distance(shop_location, delivery_location).meters
        minutes_to_deliver = (dist / (driver_km_per_hour * 1000)) * 60

        next_status_time = last_status_time + datetime.timedelta(seconds=minutes_to_deliver*60)
        other_statuses.add({
            "id": order["id"],
            "updatedAt": next_status_time.isoformat(),
            "status": "DELIVERED"
        })

        delivery_statuses.add({
            "id": order["id"],
            "updatedAt": last_status_time.isoformat(),
            "deliveryLat": str(shop_location[0]),
            "deliveryLon": str(shop_location[1])
        })

        points_to_generate = minutes_to_deliver * 60

        delivery_statuses.update(
            generate_delivery_statuses(order, last_status_time, shop_location, delivery_location, points_to_generate)
        )


@app.timer(interval=10.0)
async def order_status_poller():
    statuses_to_publish = [item for item in other_statuses if parser.parse(item["updatedAt"]) < datetime.datetime.now()]
    for status in statuses_to_publish:
        await orders_statuses_topic.send(
            key=status["id"],
            value=status
        )

        other_statuses.remove(status)

@app.timer(interval=10.0)
async def delivery_status_poller():
    statuses_to_publish = [item for item in delivery_statuses if parser.parse(item["updatedAt"]) < datetime.datetime.now()]
    for status in statuses_to_publish:
        await delivery_statuses_topic.send(
            key=status["id"],
            value=status
        )

        delivery_statuses.remove(status)