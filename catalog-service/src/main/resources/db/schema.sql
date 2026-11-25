-- Esquema de catalog_db. CREATE TABLE IF NOT EXISTS lo hace repetible en cada arranque.
-- No tiene claves foráneas hacia auth_db ni sales_db: sales-service referencia productos
-- solo por id (producto_id) y los consulta por REST.

CREATE TABLE IF NOT EXISTS categoria (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(80)  NOT NULL,
    descripcion  VARCHAR(255) NULL,
    imagen       LONGTEXT     NULL,
    activa       TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Tallas disponibles de la categoría (cualquier sistema: S, M; 38, 40; Única), en orden.
CREATE TABLE IF NOT EXISTS categoria_talla (
    categoria_id  BIGINT      NOT NULL,
    orden         INT         NOT NULL,
    talla         VARCHAR(30) NOT NULL,
    PRIMARY KEY (categoria_id, orden),
    CONSTRAINT fk_categoria_talla_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Atributos de la categoría (tipos: texto, numero, seleccion, booleano).
CREATE TABLE IF NOT EXISTS categoria_atributo (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    categoria_id  BIGINT      NOT NULL,
    clave         VARCHAR(60) NOT NULL,
    nombre        VARCHAR(80) NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    obligatorio   TINYINT(1)  NOT NULL DEFAULT 0,
    orden         INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_categoria_atributo UNIQUE (categoria_id, clave),
    CONSTRAINT fk_categoria_atributo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE CASCADE,
    CONSTRAINT ck_categoria_atributo_tipo CHECK (tipo IN ('texto', 'numero', 'seleccion', 'booleano'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS categoria_atributo_opcion (
    atributo_id  BIGINT      NOT NULL,
    orden        INT         NOT NULL,
    opcion       VARCHAR(80) NOT NULL,
    PRIMARY KEY (atributo_id, orden),
    CONSTRAINT fk_atributo_opcion_atributo FOREIGN KEY (atributo_id) REFERENCES categoria_atributo (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Medidas de la categoría (por ejemplo contorno de pecho, en cm).
CREATE TABLE IF NOT EXISTS categoria_medida (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    categoria_id        BIGINT      NOT NULL,
    clave               VARCHAR(60) NOT NULL,
    nombre              VARCHAR(80) NOT NULL,
    unidad              VARCHAR(20) NOT NULL DEFAULT 'cm',
    solicitar_cliente   TINYINT(1)  NOT NULL DEFAULT 0,
    registrar_producto  TINYINT(1)  NOT NULL DEFAULT 1,
    orden               INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_categoria_medida UNIQUE (categoria_id, clave),
    CONSTRAINT fk_categoria_medida_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS producto (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    codigo               VARCHAR(30)  NOT NULL,
    nombre               VARCHAR(120) NOT NULL,
    descripcion          TEXT         NULL,
    precio               INT          NOT NULL,
    descuento            INT          NOT NULL DEFAULT 0,
    color                VARCHAR(40)  NULL,
    categoria_id         BIGINT       NOT NULL,
    imagen               LONGTEXT     NULL,
    stock_critico        INT          NOT NULL DEFAULT 5,
    activo               TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_creacion       DATETIME     NOT NULL,
    fecha_actualizacion  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_descuento CHECK (descuento BETWEEN 0 AND 100),
    CONSTRAINT ck_producto_stock_critico CHECK (stock_critico >= 0),
    INDEX idx_producto_categoria (categoria_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Valores de los atributos de cada producto (clave definida en categoria_atributo).
CREATE TABLE IF NOT EXISTS producto_atributo (
    producto_id  BIGINT       NOT NULL,
    clave        VARCHAR(60)  NOT NULL,
    valor        VARCHAR(255) NOT NULL,
    PRIMARY KEY (producto_id, clave),
    CONSTRAINT fk_producto_atributo_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Variante por talla: stock individual. El stock total del producto es la suma de sus variantes.
CREATE TABLE IF NOT EXISTS producto_variante (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    producto_id  BIGINT      NOT NULL,
    talla        VARCHAR(30) NOT NULL,
    stock        INT         NOT NULL DEFAULT 0,
    version      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_variante_talla UNIQUE (producto_id, talla),
    CONSTRAINT fk_variante_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT ck_variante_stock CHECK (stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Medidas de cada talla (clave definida en categoria_medida).
CREATE TABLE IF NOT EXISTS variante_medida (
    variante_id  BIGINT        NOT NULL,
    clave        VARCHAR(60)   NOT NULL,
    valor        DECIMAL(8, 2) NOT NULL,
    PRIMARY KEY (variante_id, clave),
    CONSTRAINT fk_variante_medida_variante FOREIGN KEY (variante_id) REFERENCES producto_variante (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------------
-- Operaciones internas de inventario (etapa 2).
-- Cada operación aplicada con éxito deja un registro. La restricción única (tipo, clave) impide aplicar dos
-- veces el mismo descuento, reposición o reversión (idempotencia), incluso con solicitudes simultáneas.
-- Las operaciones que fallan se revierten completas y no dejan registro.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimiento_inventario (
    id      BIGINT      NOT NULL AUTO_INCREMENT,
    tipo    VARCHAR(15) NOT NULL,
    clave   VARCHAR(64) NOT NULL,
    huella  CHAR(64)    NOT NULL,
    fecha   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_movimiento_tipo_clave UNIQUE (tipo, clave),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('DESCUENTO', 'REPOSICION', 'REVERSION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Detalle por variante. No tiene clave foránea hacia producto_variante a propósito: el historial se conserva
-- aunque una talla se elimine del producto.
CREATE TABLE IF NOT EXISTS movimiento_inventario_detalle (
    movimiento_id  BIGINT      NOT NULL,
    producto_id    BIGINT      NOT NULL,
    variante_id    BIGINT      NOT NULL,
    talla          VARCHAR(30) NOT NULL,
    cantidad       INT         NOT NULL,
    stock_despues  INT         NOT NULL,
    PRIMARY KEY (movimiento_id, variante_id),
    CONSTRAINT fk_movimiento_detalle_movimiento FOREIGN KEY (movimiento_id)
        REFERENCES movimiento_inventario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
