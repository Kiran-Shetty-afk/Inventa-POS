CREATE DATABASE IF NOT EXISTS userdb;
CREATE DATABASE IF NOT EXISTS inventorydb;
CREATE DATABASE IF NOT EXISTS salesdb;
CREATE DATABASE IF NOT EXISTS billingdb;

GRANT ALL PRIVILEGES ON userdb.* TO 'inventa_app'@'%';
GRANT ALL PRIVILEGES ON inventorydb.* TO 'inventa_app'@'%';
GRANT ALL PRIVILEGES ON salesdb.* TO 'inventa_app'@'%';
GRANT ALL PRIVILEGES ON billingdb.* TO 'inventa_app'@'%';

FLUSH PRIVILEGES;
