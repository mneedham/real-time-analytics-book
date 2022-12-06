curl -s -X PUT -H  "Content-Type:application/json" http://debezium:8083/connectors/mysql/config \
    -d '{
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": 3306,
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.name": "mysql",
    "database.server.id": "223344",
    "database.allowPublicKeyRetrieval": true,
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "mysql-history",
    "database.include.list": "pizzashop",
    "time.precision.mode": "connect",
    "include.schema.changes": false
 }'