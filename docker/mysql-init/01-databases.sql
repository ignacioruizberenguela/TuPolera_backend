-- Se ejecuta una sola vez al crear el contenedor de MySQL.
-- Una instancia, tres bases de datos lógicas independientes (una por microservicio).
CREATE DATABASE IF NOT EXISTS auth_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS catalog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS sales_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- El usuario "tupolera" lo crea la imagen oficial con MYSQL_USER / MYSQL_PASSWORD.
GRANT ALL PRIVILEGES ON auth_db.*    TO 'tupolera'@'%';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'tupolera'@'%';
GRANT ALL PRIVILEGES ON sales_db.*   TO 'tupolera'@'%';
FLUSH PRIVILEGES;
