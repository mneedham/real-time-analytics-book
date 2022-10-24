import os
from flask import Flask
from flask_cors import CORS
from pinotdb import connect

pinot_host = os.environ.get("PINOT_SERVER", "pinot-broker")
pinot_port = os.environ.get("PINOT_PORT", 8099)
conn = connect(pinot_host, pinot_port)

app = Flask(__name__)
CORS(app)

@app.route('/users')
def users():
    query = """
    select DISTINCT userId AS userId
    FROM orders
    LIMIT 50
    """

    curs = conn.cursor()

    curs.execute(query)

    response = [
        {"userId": row[0]}
        for row in curs
    ]

    curs.close()

    return response

@app.route('/users/<user_id>/orders')
def users_orders(user_id):
    query = """
    select id, price, ToDateTime(ts, 'YYYY-MM-dd HH:mm:ss') AS ts
    FROM orders_enriched
    WHERE userId = %(userId)s
    LIMIT 50
    """

    curs = conn.cursor()

    curs.execute(query, {"userId": user_id})

    response = [
        {"id": row[0], "price": row[1], "ts": row[2]}
        for row in curs
    ]

    curs.close()

    return response

@app.route('/orders/<order_id>')
def orders(order_id):
    curs = conn.cursor()

    query = """
    select userId
    FROM orders_enriched
    WHERE id = %(orderId)s
    LIMIT 1
    """

    curs.execute(query, {"orderId": order_id})

    user_ids = [row[0] for row in curs]

    query = """
    select "product.name" AS product, 
           "product.price" AS price,
           "product.image" AS image,
           "orderItem.quantity" AS quantity
    from order_items_enriched 
    WHERE orderId = %(orderId)s
    limit 20
    """

    curs.execute(query, {"orderId": order_id})

    products = [
        {
          "product": row[0], 
          "price": row[1],
          "image": row[2],
          "quantity": row[3]
        }
        for row in curs
    ]

    query = """
    select ToDateTime(ts, 'YYYY-MM-dd HH:mm:ss') AS ts, status, userId
    FROM orders_enriched
    WHERE id = %(orderId)s
    ORDER BY ts DESC
    LIMIT 50    
    option(skipUpsert=true)
    """

    curs.execute(query, {"orderId": order_id})

    statuses = [
        {"timestamp": row[0], "status": row[1]}
        for row in curs
    ]

    curs.close()

    return {
        "userId": user_ids[0] if len(user_ids) > 0 else "",
        "statuses": statuses,
        "products": products
    }


if __name__ == "__main__":
    app.run(debug=True)
