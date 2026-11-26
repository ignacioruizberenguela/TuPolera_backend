# sales-service (Etapa 1A: base, cálculos y despacho)

Puerto **8083** · Base de datos **`sales_db`** · Java 21 · Spring Boot 3.5.7

Gestiona compras, boletas, despacho y reportes de Tu Polera. Se accede solo a través del API Gateway
(`http://localhost:8080`).

> **Estado:** Etapa 1A escrita y revisada, **pendiente de compilar y probar localmente**. Las compras, boletas,
> clientes HTTP (catalog y auth), idempotencia y compensación de inventario llegan en la **Etapa 1B**; los reportes,
> Swagger completo, Postman y la verificación final, en la **Etapa 2**.

## Qué incluye la Etapa 1A

- Proyecto Spring Boot independiente (Maven Wrapper incluido) con conexión a `sales_db` y `ddl-auto=none`.
- Seguridad JWT igual que catalog-service: valida firma, expiración y emisor con `JWT_SECRET` y `JWT_ISSUER`
  (los mismos de auth-service); el claim `rol` se convierte en `ROLE_ADMINISTRADOR`, `ROLE_VENDEDOR` o `ROLE_CLIENTE`.
- Esquema completo (`schema.sql`): `configuracion_despacho`, `boleta`, `boleta_detalle` y `boleta_evento`, con claves
  únicas, índices y restricciones CHECK. Las tablas de boletas aún no se usan desde código (Etapa 1B).
- Cálculos reproducibles: precio final, neto/IVA (`Dinero`), distancia Haversine (`Geo`) y precio de despacho
  (`PrecioDespacho`).
- Configuración de despacho persistida en MySQL (reemplaza a `localStorage`) y cotización de despacho.

## Endpoints de la Etapa 1A (por el Gateway)

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/v1/despacho/configuracion` | Cualquier usuario autenticado |
| PUT | `/api/v1/despacho/configuracion` | Solo ADMINISTRADOR |
| POST | `/api/v1/despacho/cotizacion` | Cualquier usuario autenticado |

Sin token: 401. Rol insuficiente: 403. Datos inválidos: 400 con `detalles` (mismo formato de error que los demás servicios).

### Cotización: `POST /api/v1/despacho/cotizacion`

El frontend (Google Maps) envía la dirección legible y las coordenadas del punto elegido. **No se envía distancia ni
precio**: el backend los calcula.

```json
{ "direccion": "Av. Providencia 1234, Providencia, Santiago", "latitud": -33.4251, "longitud": -70.6167 }
```
```json
{ "direccion": "Av. Providencia 1234, Providencia, Santiago", "latitud": -33.4251, "longitud": -70.6167,
  "distanciaKm": 3.421, "tipoCalculo": "HAVERSINE_LINEA_RECTA", "precio": 3500,
  "origen": { "nombre": "Bodega de prueba Tu Polera", "direccion": "Calle Ficticia 1234, Santiago, Región Metropolitana",
              "latitud": -33.4372, "longitud": -70.6506, "esUbicacionPrueba": true } }
```

Validaciones: dirección no vacía (se normalizan los espacios), latitud y longitud obligatorias, números finitos (ni NaN ni
infinito), latitud entre -90 y 90, longitud entre -180 y 180 y dentro del rectángulo aproximado de Chile (el mismo del
frontend: latitud -56,5 a -17,4 y longitud -110 a -66; no es un polígono exacto del país). Fuera de eso: 400.

### Configuración: `PUT /api/v1/despacho/configuracion` (solo ADMINISTRADOR)

```json
{ "nombreBodega": "Bodega Central", "direccionBodega": "Av. Siempre Viva 742, Santiago",
  "latitudBodega": -33.45, "longitudBodega": -70.66, "precioBase": 3500, "costoPorKm": 200,
  "tarifaMinima": 3000, "tarifaMaxima": 18000, "esUbicacionPrueba": false }
```
`tarifaMinima` y `tarifaMaxima` son opcionales (si se omiten, no hay límite). La máxima no puede ser menor que la mínima y
la bodega debe estar dentro de Chile. Se registra el id del administrador que hizo el cambio.

Valores iniciales (bodega de prueba, ficticia): base $2.990, $150 por km, mínimo $2.990, máximo $15.000,
latitud -33,4372, longitud -70,6506, `esUbicacionPrueba = true`.

## Cómo se calcula (en español sencillo)

1. **Distancia (Haversine):** distancia geográfica en **línea recta** entre la bodega y el destino. **No es la distancia
   real por calles** (el recorrido en auto suele ser mayor). Es una aproximación para esta primera versión; no usa Google
   Routes ni servicios externos.
2. **Precio:** `precio = precioBase + costoPorKm × max(0, distanciaKm)`; luego se aplica la tarifa mínima y la máxima, y
   se redondea a la decena más cercana (el .5 sube). Es la misma fórmula de `despacho.js`; se usa `double` como
   JavaScript para obtener resultados idénticos. La distancia que se muestra va redondeada a 3 decimales, pero el
   precio se calcula con la distancia exacta.

| Distancia | Precio (configuración inicial) |
|---:|---:|
| 0 km | $2.990 |
| 10 km | $4.490 |
| 12,34 km | $4.840 |
| 25 km | $6.740 |
| 100 km | $15.000 |

3. **Dinero (CLP, enteros):** precio final = `Math.round(precio - precio × descuento / 100.0)`. Los precios ya incluyen
   IVA 19%: `neto = (200 × bruto + 119) / 238` e `iva = bruto - neto`. El IVA se calcula **solo sobre el subtotal de
   productos**; el despacho va aparte y `total = subtotal de productos + despacho`.

## Variables de entorno

Se usan las que ya existen en tu `.env` (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`) y, en la Etapa 1B,
`AUTH_SERVICE_URL`, `CATALOG_SERVICE_URL` e `INTERNAL_API_KEY`. **No hace falta cambiar el `.env`.**

Opcional: `SALES_DB_URL`. En desarrollo (perfil por defecto) se usa
`jdbc:mysql://localhost:3306/sales_db?...`; en producción es obligatoria. (No se modificó `.env.example` porque es un
archivo compartido.)

## Cómo ejecutar y probar (Windows, desde la carpeta `backend`)

```powershell
# Pruebas
.\scripts\run-service.ps1 sales-service test

# Ejecutar (MySQL levantado; el Gateway y auth-service para probar por el Gateway)
.\scripts\run-service.ps1 sales-service
```

| Grupo | Pruebas | Requiere |
|---|---:|---|
| `DineroTest` (precio final, neto/IVA; compara contra BigDecimal en 300.000 montos) | 16 | nada |
| `GeoTest` (Haversine contra valores del frontend, rangos, Chile) | 6 | nada |
| `PrecioDespachoTest` (valores obtenidos ejecutando `despacho.js`) | 19 | nada |
| `DespachoServiceTest` (cotización, configuración, validaciones) | 15 | nada |
| `DespachoWebTest` (401, 403, 200, 400) | 7 | nada |
| `EstadoDockerTest` (solo informa si hay Docker) | 1 | nada |
| **Subtotal sin Docker** | **64** | |
| `SalesDbIntegracionTest` (MySQL 8 real: esquema, configuración, restricciones) | 17 | **Docker Desktop en ejecución** |
| **Total con Docker** | **81** | |

Igual que en catalog-service: si Docker no está disponible, las 17 pruebas de integración **se omiten**. Al final se
imprime el recuadro `PRUEBAS DE INTEGRACION CON DOCKER` (ejecutadas, aprobadas y **OMITIDAS**) y queda en
`target\resumen-integracion-docker.txt`. Un `BUILD SUCCESS` con pruebas omitidas no valida el esquema real.

Comprobaciones manuales (con sales-service, auth-service y el Gateway levantados; usa el token de login de auth):

1. `GET http://localhost:8080/api/v1/despacho/configuracion` sin token → 401; con token → 200 (base 2990).
2. `POST /api/v1/despacho/cotizacion` con Providencia (-33.4251, -70.6167) → `precio` 3500, `distanciaKm` 3.421.
3. El mismo POST con Buenos Aires (-34.6037, -58.3816) → 400.
4. `PUT /api/v1/despacho/configuracion` con el token de CLIENTE → 403; con el de ADMINISTRADOR → 200.
5. En MySQL: `SHOW TABLES FROM sales_db;` → 4 tablas; `SELECT * FROM sales_db.configuracion_despacho;` → 1 fila.

## Decisiones de diseño

- La configuración vive en MySQL (una sola fila, `id = 1`); si faltara, se crea con los valores iniciales.
- Las fechas se guardan en UTC; los reportes (Etapa 2) las convertirán a `America/Santiago`.
- La boleta guardará una fotografía histórica del comprador, del destino (dirección, coordenadas, distancia y tipo de
  cálculo) y de la bodega usada, además de los productos (nombre, imagen, precios, talla), para no depender de que
  auth, catalog o Google Maps cambien después.
- El esquema impone combinaciones coherentes de estado e inventario y que `total = subtotal + despacho` y
  `neto + iva = subtotal`. Si en la Etapa 1B hiciera falta cambiar columnas de `boleta*`, como `schema.sql` usa
  `CREATE TABLE IF NOT EXISTS`, bastaría borrar esas tablas **solo de sales_db** en tu entorno de desarrollo.
