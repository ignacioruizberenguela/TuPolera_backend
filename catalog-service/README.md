# catalog-service (etapas 1 y 2: modelo, CRUD, seguridad y operaciones internas de inventario)

Puerto **8082** · Base de datos **`catalog_db`** · Java 21 · Spring Boot 3.5.7

Administra categorías configurables, productos con variantes por talla y stock.
Solo se accede a través del API Gateway (`http://localhost:8080`).

> **Estado:** código escrito y revisado, **aún sin compilar ni ejecutar** por quien lo desarrolló.
> Sigue la sección "Cómo verificar" y envía el resultado de Maven si algo falla.
> **Etapa 2** (operaciones internas `/internal/**`) escrita y revisada, pendiente de verificar localmente: ver la sección
> "Operaciones internas de inventario" y "Pruebas".

## Modelo (catalog_db)

| Tabla | Contenido |
|---|---|
| `categoria` | id, nombre (único), descripcion, imagen, activa |
| `categoria_talla` | tallas de la categoría, en orden (cualquier sistema: S, M; 38, 40; Única) |
| `categoria_atributo` / `categoria_atributo_opcion` | atributos (texto, numero, seleccion, booleano), obligatorio, opciones |
| `categoria_medida` | medidas (nombre, unidad, solicitarCliente, registrarProducto) |
| `producto` | codigo (único), nombre, descripcion, precio (CLP, IVA incluido), descuento %, color, categoria_id, imagen, stock_critico (5 por defecto), activo, fechas |
| `producto_atributo` | valor de cada atributo del producto (por clave) |
| `producto_variante` | **stock por talla** (+ `version` para concurrencia optimista) |
| `variante_medida` | medidas de cada talla |

Sin claves foráneas hacia `auth_db` ni `sales_db`. El **stock general** de un producto es la suma de sus variantes (se calcula, no se guarda).

## Endpoints (por el Gateway)

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/v1/productos` | Público (solo activos). VENDEDOR/ADMINISTRADOR: `?incluirInactivos=true` |
| GET | `/api/v1/productos/{id}` | Público (activos) |
| GET | `/api/v1/productos/categoria/{categoriaId}` | Público |
| GET | `/api/v1/productos/criticos` | ADMINISTRADOR |
| POST / PUT / DELETE | `/api/v1/productos[/{id}]` | ADMINISTRADOR (DELETE = desactivación lógica) |
| GET | `/api/v1/categorias[/{id}]` | Público (activas). VENDEDOR/ADMINISTRADOR: `?incluirInactivas=true` |
| POST / PUT / DELETE | `/api/v1/categorias[/{id}]` | ADMINISTRADOR |

Forma del producto (compatible con el frontend): `tallas` es un objeto `talla -> {stock, medidas}` y
`atributos` un objeto `clave -> valor`; además incluye `codigo`, `stock`, `stockCritico`, `critico`, `tallasCriticas`,
`activo`, `precioFinal` (precio con descuento) y fechas.

### Reglas

- Producto: `codigo` único (≥ 3), nombre obligatorio, `precio ≥ 0`, `descuento` 0–100, stock por talla entero ≥ 0.
- Las tallas deben pertenecer a la categoría; se exigen las medidas que la categoría marca como `registrarProducto` (> 0);
  los atributos se validan por tipo, opciones y obligatoriedad.
- **Stock crítico:** una talla es crítica si su stock ≤ `stockCritico` del producto (por defecto 5). `/criticos` lista los
  productos activos con al menos una talla crítica (incluye el caso en que el stock total llega al umbral).
- `DELETE` de producto desactiva (puede tener boletas históricas). `PUT` con `activo: true` lo reactiva.
- `DELETE` de categoría: con productos activos → 409; solo con inactivos → se desactiva; sin productos → se elimina.
- Editar un producto reemplaza su stock con el valor enviado. Si una venta cambia ese stock mientras se guarda, la
  edición falla con 409 en lugar de pisarlo (control optimista por `version`).
- Los datos de ejemplo (3 categorías y 6 productos del frontend) se cargan solo con el perfil `dev`.

## Seguridad

Valida el JWT con `JWT_SECRET` y `JWT_ISSUER` (los mismos que auth-service y el Gateway) y aplica sus propios roles
con `@PreAuthorize`. El rol sale del claim `rol` del token, nunca del cuerpo de la solicitud. CORS lo maneja solo el Gateway.

## Cómo verificar (Windows, desde la carpeta `backend`)

```powershell
# 1) Pruebas (no necesitan MySQL)
.\scripts\run-service.ps1 catalog-service test

# 2) Ejecutar (con MySQL levantado y auth-service y api-gateway corriendo)
.\scripts\run-service.ps1 catalog-service
```

No hay variables nuevas en el `.env` para esta etapa: usa `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `JWT_ISSUER`.
(`CATALOG_DB_URL` es opcional en dev y obligatoria en `prod`.)

Pruebas: ver la sección "Pruebas" al final.

Comprobaciones manuales (con los tres servicios levantados):

1. `GET http://localhost:8080/api/v1/productos` → 6 productos de ejemplo (sin token).
2. `GET http://localhost:8080/api/v1/categorias` → 3 categorías con tallas, atributos y medidas.
3. `GET http://localhost:8080/api/v1/productos/criticos` sin token → 401; con token de CLIENTE/VENDEDOR → 403; con ADMINISTRADOR → 200.
4. `POST /api/v1/categorias` con el token de ADMINISTRADOR → 201; con el de CLIENTE → 403.
5. `http://localhost:8080/swagger-ui.html` → seleccionar `catalog-service`.
6. En MySQL: `SELECT COUNT(*) FROM catalog_db.producto;` → 6.

Colección Postman: `postman/tupolera-catalog.postman_collection.json`.

---

## Operaciones internas de inventario (etapa 2)

Las usa **sales-service** (no el frontend). Están bajo `/internal/**`, **no pasan por el Gateway** (no hay ruta, y el
Gateway además borra la cabecera `X-Internal-Key`), no aparecen en Swagger y se llaman directo a `http://localhost:8082`.
En producción el puerto 8082 debe quedar sin acceso público (solo accesible desde la red interna / otros servicios).

### Configurar la clave `INTERNAL_API_KEY`

1. Genera un valor aleatorio de al menos 32 caracteres, por ejemplo (PowerShell):
   `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`
2. Agrégalo a tu `.env` (en `backend\`, junto a `JWT_SECRET`):  `INTERNAL_API_KEY=<el valor generado>`
3. Reinicia `catalog-service`. sales-service usará el mismo valor cuando lo implementemos.
4. No lo subas a GitHub ni lo compartas. El servicio nunca lo escribe en logs ni en respuestas.

Si la variable no existe o tiene menos de 32 caracteres, `catalog-service` arranca igual (con un aviso en el log) pero
**todas** las rutas `/internal/**` responden 401. Un JWT (aunque sea de administrador) **no** da acceso a `/internal`.

### Cabecera y formato de error

Todas las solicitudes llevan `X-Internal-Key: <INTERNAL_API_KEY>` y `Content-Type: application/json`.

- Datos inválidos (cantidad ≤ 0 o > 10000, lista vacía o de más de 100, referencia inválida...): **400** con `detalles`.
- Sin clave o clave incorrecta: **401**.
- Producto o talla inexistente: **404**. Producto inactivo, stock insuficiente o conflicto de idempotencia: **409**.
- En 404/409 de inventario el cuerpo trae `errores` con un elemento por ítem con problema (todos a la vez):

```json
{ "status": 409, "message": "La operación no se pudo aplicar: 1 ítem(s) con problemas. No se modificó ningún stock.",
  "errores": [ { "productoId": 1, "talla": "S", "codigo": "STOCK_INSUFICIENTE",
                 "mensaje": "Stock insuficiente para el producto 1 talla S: pediste 99 y hay 5.", "disponible": 5 } ] }
```

Códigos: `PRODUCTO_NO_ENCONTRADO`, `PRODUCTO_INACTIVO`, `VARIANTE_NO_ENCONTRADA`, `STOCK_INSUFICIENTE`, `LIMITE_STOCK`,
`CONFLICTO_IDEMPOTENCIA`, `REFERENCIA_REVERTIDA`, `DESCUENTO_NO_ENCONTRADO`.

### 1. Consulta en lote — `POST /internal/inventario/consulta`

```json
{ "productoIds": [1, 2, 99] }
```
```json
{ "productos": [ { "id": 1, "codigo": "POL-001", "nombre": "Urban Red", "activo": true,
                   "precio": 12990, "descuento": 50, "precioFinal": 6495, "categoriaId": 1,
                   "variantes": [ {"id": 1, "talla": "S", "stock": 5}, {"id": 2, "talla": "M", "stock": 10} ] } ],
  "noEncontrados": [99] }
```
`precioFinal` es el precio de venta vigente: CLP, **con el descuento ya aplicado** (misma fórmula que el catálogo y el
frontend: `round(precio - precio × descuento / 100)`) y **con IVA 19% incluido** (sales-service lo desglosa sin cambiar el
valor). Los productos inactivos se devuelven con `activo: false` para que sales decida.

### 2. Descuento atómico — `POST /internal/inventario/descuento`

```json
{ "referencia": "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10",
  "items": [ {"productoId": 1, "talla": "M", "cantidad": 2}, {"productoId": 3, "talla": "S", "cantidad": 1} ] }
```
```json
{ "referencia": "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10", "tipo": "DESCUENTO", "repetida": false,
  "items": [ {"productoId": 1, "varianteId": 2, "talla": "M", "cantidad": 2, "stockActual": 8},
             {"productoId": 3, "varianteId": 31, "talla": "S", "cantidad": 1, "stockActual": 1} ] }
```
- **`referencia`**: identificador único de la operación, **generado por sales-service antes de pedir el descuento**
  (idealmente un UUID; 8–64 caracteres: letras, números, punto, guion o guion bajo). No depende del número de boleta.
- Es **todo o nada**: si cualquier ítem falla, no se descuenta ninguno.
- **Idempotente**: repetir la misma referencia con los mismos ítems devuelve el resultado original con `"repetida": true`
  y **no vuelve a descontar**. La misma referencia con ítems distintos → 409 `CONFLICTO_IDEMPOTENCIA`.
- Un intento fallido no consume la referencia: se puede reintentar.
- Un mismo producto+talla no puede repetirse en la solicitud (suma las cantidades en un solo ítem).

### 3. Reposición — `POST /internal/inventario/reposicion`

```json
{ "claveIdempotencia": "7c1d4a52-91b3-4e7f-8d20-5b6a3c9e1f44", "items": [ {"productoId": 1, "talla": "S", "cantidad": 7} ] }
```
Misma forma de respuesta que el descuento (`"tipo": "REPOSICION"`). Con la misma clave no se repone dos veces. Permite
reponer productos inactivos; el producto y la talla deben existir. Máximo 1.000.000 unidades por variante.

### 4. Reversión (compensación) — `POST /internal/inventario/descuento/{referencia}/reversion`

Sin cuerpo. Devuelve **exactamente las cantidades registradas en el descuento original** de esa referencia (no se pueden
indicar cantidades), **una sola vez**: repetirla, o llamarla en paralelo, devuelve el mismo resultado
(`"repetida": true`) sin volver a sumar. 404 `DESCUENTO_NO_ENCONTRADO` si no existe un descuento con esa referencia.
Después de revertir, repetir el descuento con esa referencia responde 409 `REFERENCIA_REVERTIDA`.

Uso previsto en sales-service: generar `referencia` → `descuento` → crear/confirmar la boleta; si la confirmación falla,
llamar a `reversion` con la misma referencia.

### Cómo se garantiza la atomicidad y la ausencia de carreras

1. Cada operación corre en **una transacción**; cualquier error la revierte completa.
2. Se validan todos los ítems antes de modificar y se reportan **todos** los problemas.
3. El descuento es un `UPDATE producto_variante SET stock = stock - :c WHERE id = :id AND stock >= :c`: MySQL bloquea la
   fila y evalúa la condición con el valor ya confirmado, así dos compras simultáneas no venden la misma unidad. Si el
   UPDATE no cambia ninguna fila, se aborta y se deshace todo.
4. Las variantes se actualizan siempre **en orden de id** (evita deadlocks) y hay reintento ante un deadlock de MySQL.
5. La tabla `movimiento_inventario` tiene clave única `(tipo, clave)`: es lo que hace que una referencia solo pueda
   aplicarse una vez aunque lleguen solicitudes idénticas a la vez. La restricción `CHECK (stock >= 0)` es una red de
   seguridad adicional.

---

## Pruebas

```powershell
.\scripts\run-service.ps1 catalog-service test
```

| Grupo | Cantidad | Requiere |
|---|---:|---|
| Etapa 1 (`ProductoServiceTest`, `CategoriaServiceTest`, `CatalogSecurityWebTest`) | 41 | nada |
| Etapa 2: lógica (`InventarioServiceTest`, repositorios simulados) | 23 | nada |
| Etapa 2: clave interna (`InternalApiKeyWebTest`, `InternalApiKeyNoConfiguradaWebTest`) | 12 | nada |
| `EstadoDockerTest` (solo informa si hay Docker) | 1 | nada |
| **Subtotal sin Docker** | **77** | |
| Etapa 2: integración con MySQL real (`InventarioIntegracionTest`) | 17 | **Docker Desktop en ejecución** |
| **Total con Docker** | **94** | |

### Importante: las pruebas de concurrencia solo cuentan si Docker corrió

`InventarioIntegracionTest` levanta un **MySQL 8 desechable** con Testcontainers (no usa ni modifica tu base de
desarrollo) y es lo único que demuestra de verdad la atomicidad y la concurrencia. Si Docker no está disponible esas
17 pruebas **se omiten** y Maven puede terminar en `BUILD SUCCESS` igualmente. Para que no se confunda:

- Al final de la ejecución se imprime un recuadro `PRUEBAS DE INTEGRACION CON DOCKER` con cuántas se **ejecutaron,
  aprobaron y OMITIERON**, y el motivo. También queda en `target\resumen-integracion-docker.txt`.
- `EstadoDockerTest` imprime al inicio `[ESTADO DOCKER] ...` (disponible o no).
- Busca en el resumen de Maven `Tests run: ..., Skipped: N`: si `Skipped` es mayor que 0, la concurrencia NO se validó.

Ejecutar solo las de integración: `.\scripts\run-service.ps1 catalog-service test` y filtra con
`cd catalog-service; .\mvnw.cmd test -Dtest=InventarioIntegracionTest` (la primera vez descarga la imagen `mysql:8.0`).

### Si Docker está en ejecución pero las pruebas de integración se omiten

Revisa el motivo en `[ESTADO DOCKER]` / `target\resumen-integracion-docker.txt`. Una causa posible es que la versión de
Testcontainers que trae Spring Boot 3.5.7 sea demasiado antigua para la API de tu Docker Desktop (error del tipo
"client version ... is too old"). En ese caso sube la versión agregando en `<properties>` del `pom.xml` una versión
más reciente de Testcontainers compatible con Boot 3.5 (propiedad `testcontainers.version`) y vuelve a ejecutar.
Si el motivo es otro, envíame el texto del recuadro.
