# Arquitectura y decisiones técnicas

## Visión general

```
React ──HTTP/JSON──► API Gateway :8080 ──► auth-service :8081 ──► auth_db
                                       ├─► catalog-service :8082 ──► catalog_db   (próxima entrega)
                                       └─► sales-service :8083 ──► sales_db       (próxima entrega)
```

El frontend solo conoce la URL del Gateway. Los microservicios no se exponen públicamente.

## Seguridad

- **JWT HS256** con Spring Security OAuth2 Resource Server (Nimbus). auth-service firma; el Gateway y cada servicio
  verifican firma, expiración y emisor (`iss`) con el mismo secreto (`JWT_SECRET`, variable de entorno ≥ 32 caracteres).
  El secreto nunca se escribe en el código ni llega al frontend. Si se filtra, hay que cambiarlo en todos los servicios
  (esto invalida todos los tokens).
- **Claims**: `sub` (id del usuario), `correo`, `nombre`, `rol`. Los servicios obtienen identidad y rol **solo** del token
  firmado; nunca del cuerpo de la solicitud ni de encabezados enviados por el cliente.
- **Dos capas**: el Gateway rechaza solicitudes sin token válido en rutas protegidas (401); cada servicio vuelve a validar
  el token y aplica sus roles (`@PreAuthorize`), respondiendo 403 si no corresponde.
- **Refresh tokens**: valor aleatorio de 256 bits; en la BD solo se guarda su hash SHA-256. Se rotan en cada renovación.
  Si se presenta uno ya usado se revocan todas las sesiones del usuario. `logout` revoca el token entregado.
  Cambiar contraseña/rol o desactivar un usuario revoca sus refresh tokens.
- **Limitación conocida**: el access token (15 min) sigue siendo válido hasta expirar aunque el usuario sea desactivado
  (catalog y sales no consultan la BD de auth). `GET /auth/me` sí verifica que el usuario siga activo.
- **Token inválido en ruta pública**: si el frontend envía un token vencido a una ruta pública (por ejemplo GET /productos),
  Spring Security responde 401. El frontend debe renovar el token o no enviarlo en esas rutas.
- **CORS**: solo en el Gateway (`CORS_ALLOWED_ORIGINS`). Los servicios lo desactivan para no duplicar encabezados.
- El Gateway elimina el encabezado `X-Internal-Key` de las solicitudes externas (se usará entre servicios).

## Datos

- Una instancia de MySQL con tres bases lógicas; cada servicio usa solo la suya.
- **Sin claves foráneas entre bases**: `boleta.usuario_id` y `detalle_boleta.producto_id` serán identificadores
  lógicos resueltos por REST. Crear FK físicas entre bases separadas no es posible ni deseable en microservicios,
  y las relaciones USUARIO–BOLETA–DETALLE–PRODUCTO–CATEGORIA del modelo lógico se mantienen por id.
- El esquema se crea con `db/schema.sql` (`ddl-auto=none`) y los datos iniciales con scripts SQL repetibles.
- **Regiones y comunas**: se generaron desde `regionesComunas.js` del frontend (no se tuvo el `comuna-region.sql` del
  Anexo 2). Ids estables según el orden del archivo. El archivo fuente contiene **16 regiones y 305 comunas**
  (Chile tiene 346 comunas): faltan comunas respecto a la división oficial. Si dispones del Anexo 2, conviene
  reemplazar `data-regiones.sql` conservando ids.
- RUN: se almacena sin puntos ni guion y se valida con dígito verificador.
- `DELETE /usuarios/{id}` desactiva (el historial de boletas debe conservar al usuario).

## Versiones

Java 21, Spring Boot 3.5.7, Spring Cloud 2025.0.0 (línea compatible con Boot 3.5; el starter se llama
`spring-cloud-starter-gateway-server-webflux` y las propiedades usan `spring.cloud.gateway.server.webflux`),
springdoc 2.8.9, MySQL 8. Estas versiones no pudieron resolverse en el entorno de desarrollo: si Maven reporta que alguna
no existe, ajusta el número de versión de parche en el `pom.xml` (propiedades `spring-cloud.version`, `springdoc.version`
y versión del parent).

## Pendiente de decidir para sales-service (acordado, no implementado aún)

- Precios con IVA incluido; el backend desglosa neto e IVA 19% sin cambiar el total publicado.
- Despacho calculado en sales-service; la distancia que envía el frontend se marca como **no verificada**; la
  arquitectura quedará preparada para obtenerla desde Google Maps en el backend.
- Pago simulado: boleta RECHAZADA no descuenta stock; APROBADA valida y descuenta; liberación de reservas si falla.
