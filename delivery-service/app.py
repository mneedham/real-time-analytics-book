import faust
import datetime
import random
from dateutil import parser
from sortedcontainers import SortedList

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


other_statuses = SortedList(key=lambda x: x["updatedAt"])

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


@app.timer(interval=10.0)
async def every_10_seconds():
    other_statuses_to_publish = [item for item in other_statuses if parser.parse(item["updatedAt"]) < datetime.datetime.now()]
    for status in other_statuses_to_publish:
        await orders_statuses_topic.send(
            key=status["id"],
            value=status
        )

        other_statuses.remove(status)