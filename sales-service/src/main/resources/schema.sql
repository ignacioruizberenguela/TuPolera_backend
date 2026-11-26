-- Esquema de sales_db. CREATE TABLE IF NOT EXISTS lo hace repetible en cada arranque (no borra nada).
-- Sin claves foráneas hacia auth_db ni catalog_db: usuarios y productos se referencian solo por su id
-- (usuario_id, producto_id) y se consultan por REST. Todas las fechas están en UTC.
-- Todos los montos son enteros en pesos chilenos (CLP).

-- ---------------------------------------------------------------------------
-- Configuración oficial del despacho: una sola fila (id = 1).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configuracion_despacho (
    id                   BIGINT       NOT NULL,
    nombre_bodega        VARCHAR(120) NOT NULL,
    direccion_bodega     VARCHAR(255) NOT NULL,
    latitud_bodega       DOUBLE       NOT NULL,
    longitud_bodega      DOUBLE       NOT NULL,
    precio_base          BIGINT       NOT NULL,
    costo_por_km         BIGINT       NOT NULL,
    tarifa_minima        BIGINT       NULL,
    tarifa_maxima        BIGINT       NULL,
    es_ubicacion_prueba  TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_actualizacion  DATETIME     NOT NULL,
    actualizado_por      BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_config_fila_unica CHECK (id = 1),
    CONSTRAINT ck_config_montos CHECK (precio_base >= 0 AND costo_por_km >= 0),
    CONSTRAINT ck_config_tarifas CHECK (
        (tarifa_minima IS NULL OR tarifa_minima >= 0)
        AND (tarifa_maxima IS NULL OR tarifa_maxima >= 0)
        AND (tarifa_minima IS NULL OR tarifa_maxima IS NULL OR tarifa_maxima >= tarifa_minima)),
    CONSTRAINT ck_config_coordenadas CHECK (
        latitud_bodega BETWEEN -90 AND 90 AND longitud_bodega BETWEEN -180 AND 180)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Valores iniciales: la bodega de prueba (ficticia) de Tu Polera. Es editable desde la API por un administrador.
INSERT IGNORE INTO configuracion_despacho
    (id, nombre_bodega, direccion_bodega, latitud_bodega, longitud_bodega, precio_base, costo_por_km,
     tarifa_minima, tarifa_maxima, es_ubicacion_prueba, fecha_actualizacion, actualizado_por)
VALUES
    (1, 'Bodega de prueba Tu Polera', 'Calle Ficticia 1234, Santiago, Región Metropolitana',
     -33.4372, -70.6506, 2990, 150, 2990, 15000, 1, UTC_TIMESTAMP(), NULL);

-- ---------------------------------------------------------------------------
-- Boleta: una compra (aprobada, rechazada o pendiente), con una FOTOGRAFÍA histórica del comprador, del destino
-- y de la bodega usada. No depende de que auth, catalog ni Google Maps sigan disponibles o sin cambios.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS boleta (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    -- BOL-0001: se asigna a partir del id justo después de insertar (mientras tanto queda NULL).
    numero_boleta            VARCHAR(20)   NULL,
    -- Identificador de la operación (TP-<uuid>). También es la referencia que se envía a catalog-service
    -- para descontar y revertir el inventario.
    numero_operacion         VARCHAR(64)   NOT NULL,
    usuario_id               BIGINT        NOT NULL,
    comprador_nombre         VARCHAR(200)  NOT NULL,
    comprador_correo         VARCHAR(120)  NOT NULL,
    comprador_run            VARCHAR(12)   NULL,
    fecha                    DATETIME      NOT NULL,
    fecha_actualizacion      DATETIME      NOT NULL,

    estado                   VARCHAR(12)   NOT NULL,
    estado_inventario        VARCHAR(15)   NOT NULL,
    motivo_rechazo           VARCHAR(255)  NULL,
    -- Resultado del pago SIMULADO que eligió el cliente en el frontend (no es una autorización bancaria real).
    resultado_pago           VARCHAR(10)   NOT NULL,
    referencia_descuento     VARCHAR(64)   NULL,

    -- Idempotencia: la misma clave del mismo usuario no puede crear dos boletas.
    clave_idempotencia       VARCHAR(64)   NOT NULL,
    huella_solicitud         CHAR(64)      NOT NULL,

    -- Montos (CLP). subtotal = productos con IVA incluido; neto e iva se calculan SOLO sobre los productos;
    -- el despacho va aparte; total = subtotal + precio_despacho.
    subtotal                 BIGINT        NOT NULL,
    precio_despacho          BIGINT        NOT NULL,
    total                    BIGINT        NOT NULL,
    neto                     BIGINT        NOT NULL,
    iva                      BIGINT        NOT NULL,

    -- Fotografía del destino elegido en el mapa y del cálculo realizado.
    despacho_direccion       VARCHAR(255)  NOT NULL,
    despacho_latitud         DOUBLE        NOT NULL,
    despacho_longitud        DOUBLE        NOT NULL,
    despacho_distancia_km    DECIMAL(10,3) NOT NULL,
    despacho_tipo_calculo    VARCHAR(30)   NOT NULL,
    -- Fotografía de la bodega usada como origen.
    origen_nombre            VARCHAR(120)  NOT NULL,
    origen_direccion         VARCHAR(255)  NOT NULL,
    origen_latitud           DOUBLE        NOT NULL,
    origen_longitud          DOUBLE        NOT NULL,

    -- Control de concurrencia optimista: evita que dos procesos pisen el estado de la misma boleta.
    version                  BIGINT        NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT uk_boleta_numero UNIQUE (numero_boleta),
    CONSTRAINT uk_boleta_operacion UNIQUE (numero_operacion),
    CONSTRAINT uk_boleta_referencia_descuento UNIQUE (referencia_descuento),
    CONSTRAINT uk_boleta_usuario_clave UNIQUE (usuario_id, clave_idempotencia),
    INDEX idx_boleta_usuario_fecha (usuario_id, fecha),
    INDEX idx_boleta_estado_fecha (estado, fecha),

    CONSTRAINT ck_boleta_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_boleta_estado_inventario CHECK (
        estado_inventario IN ('NO_APLICA', 'PENDIENTE', 'DESCONTADO', 'REVERTIDO', 'INCIERTO')),
    CONSTRAINT ck_boleta_resultado_pago CHECK (resultado_pago IN ('APROBADO', 'RECHAZADO')),
    -- Combinaciones coherentes: una boleta aprobada tiene el inventario descontado; una pendiente tiene el
    -- inventario por confirmar; una rechazada no tiene descuento vigente.
    CONSTRAINT ck_boleta_aprobada_descontada CHECK (estado <> 'APROBADA' OR estado_inventario = 'DESCONTADO'),
    CONSTRAINT ck_boleta_pendiente_por_confirmar CHECK (
        estado <> 'PENDIENTE' OR estado_inventario IN ('PENDIENTE', 'INCIERTO')),
    CONSTRAINT ck_boleta_rechazada_sin_descuento CHECK (
        estado <> 'RECHAZADA' OR estado_inventario IN ('NO_APLICA', 'REVERTIDO')),
    CONSTRAINT ck_boleta_montos CHECK (
        subtotal >= 0 AND precio_despacho >= 0 AND neto >= 0 AND iva >= 0
        AND total = subtotal + precio_despacho AND neto + iva = subtotal),
    CONSTRAINT ck_boleta_coordenadas CHECK (
        despacho_latitud BETWEEN -90 AND 90 AND despacho_longitud BETWEEN -180 AND 180
        AND despacho_distancia_km >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------------
-- Detalle de la boleta: fotografía de cada producto comprado (no cambia aunque el producto se modifique).
-- producto_id no tiene clave foránea: el producto vive en catalog_db.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS boleta_detalle (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    boleta_id          BIGINT       NOT NULL,
    producto_id        BIGINT       NOT NULL,
    codigo_producto    VARCHAR(30)  NULL,
    nombre_producto    VARCHAR(120) NOT NULL,
    -- Imagen original del catálogo si pesa hasta 1 MB; si es más grande no se guarda (imagen_disponible = 0).
    imagen             LONGTEXT     NULL,
    imagen_disponible  TINYINT(1)   NOT NULL DEFAULT 0,
    talla              VARCHAR(30)  NOT NULL,
    precio_original    BIGINT       NOT NULL,
    descuento          INT          NOT NULL DEFAULT 0,
    precio_final       BIGINT       NOT NULL,
    cantidad           INT          NOT NULL,
    subtotal           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_detalle_boleta_producto_talla UNIQUE (boleta_id, producto_id, talla),
    CONSTRAINT fk_detalle_boleta FOREIGN KEY (boleta_id) REFERENCES boleta (id) ON DELETE CASCADE,
    INDEX idx_detalle_producto (producto_id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precios CHECK (
        precio_original >= 0 AND precio_final >= 0 AND descuento BETWEEN 0 AND 100
        AND subtotal = precio_final * cantidad),
    CONSTRAINT ck_detalle_imagen CHECK (imagen_disponible = 1 OR imagen IS NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------------
-- Eventos de una boleta: bitácora de lo que ocurrió con la operación (para auditar y retomar operaciones
-- inciertas). Nunca guarda JWT, claves internas ni datos sensibles.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS boleta_evento (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    boleta_id  BIGINT       NOT NULL,
    fecha      DATETIME     NOT NULL,
    tipo       VARCHAR(40)  NOT NULL,
    detalle    VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evento_boleta FOREIGN KEY (boleta_id) REFERENCES boleta (id) ON DELETE CASCADE,
    INDEX idx_evento_boleta_fecha (boleta_id, fecha)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
