-- Esquema de auth_db. CREATE TABLE IF NOT EXISTS lo hace repetible en cada arranque.
-- Este esquema es independiente: no tiene claves foráneas hacia otras bases de datos
-- (catalog_db y sales_db se relacionan con usuario solo mediante el id, vía REST).

CREATE TABLE IF NOT EXISTS region (
    id      BIGINT       NOT NULL,
    nombre  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS comuna (
    id         BIGINT       NOT NULL,
    region_id  BIGINT       NOT NULL,
    nombre     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comuna_region FOREIGN KEY (region_id) REFERENCES region (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS usuario (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    run               VARCHAR(9)   NOT NULL,
    nombre            VARCHAR(80)  NOT NULL,
    apellidos         VARCHAR(100) NULL,
    correo            VARCHAR(120) NOT NULL,
    password          VARCHAR(100) NOT NULL,
    fecha_nacimiento  DATE         NULL,
    direccion         VARCHAR(200) NULL,
    region_id         BIGINT       NOT NULL,
    comuna_id         BIGINT       NOT NULL,
    rol               VARCHAR(20)  NOT NULL,
    activo            TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_run UNIQUE (run),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_region FOREIGN KEY (region_id) REFERENCES region (id),
    CONSTRAINT fk_usuario_comuna FOREIGN KEY (comuna_id) REFERENCES comuna (id),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR', 'CLIENTE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Los refresh tokens se guardan como hash SHA-256 (nunca el valor original).
CREATE TABLE IF NOT EXISTS refresh_token (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id        BIGINT      NOT NULL,
    token_hash        VARCHAR(64) NOT NULL,
    fecha_expiracion  DATETIME    NOT NULL,
    revocado          TINYINT(1)  NOT NULL DEFAULT 0,
    fecha_creacion    DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX idx_refresh_usuario (usuario_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
