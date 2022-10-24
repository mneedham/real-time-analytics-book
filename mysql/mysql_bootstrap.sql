CREATE DATABASE IF NOT EXISTS pizzashop;
USE pizzashop;

GRANT ALL PRIVILEGES ON pizzashop.* TO 'mysqluser';
GRANT FILE on *.* to 'mysqluser';

CREATE USER 'debezium' IDENTIFIED WITH mysql_native_password BY 'dbz';

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium';

FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS pizzashop.users
(
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    residence VARCHAR(500),
    lat DECIMAL(10, 8),
    lon DECIMAL(10, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pizzashop.products
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    description VARCHAR(500),
    category VARCHAR(100),
    price FLOAT,
    image VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

LOAD DATA INFILE '/var/lib/mysql-files/data/products.csv' 
INTO TABLE pizzashop.products 
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
IGNORE 1 LINES
(name,description,price,category,image);

LOAD DATA INFILE '/var/lib/mysql-files/data/users.csv' 
INTO TABLE pizzashop.users 
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
IGNORE 1 LINES
(first_name,last_name,email,residence,lat,lon);