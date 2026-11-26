# Tu Polera – Backend (entrega 1 de 3: API Gateway + auth-service)

Arquitectura: **frontend React → API Gateway → 3 microservicios (auth, catalog, sales) → MySQL**.

> **Estado de verificación (importante):** este código se escribió sin poder ejecutar Maven ni MySQL.
> Solo se hizo una revisión de sintaxis con `javac` (sin errores de sintaxis; los demás errores esperados son las
> librerías que no se pudieron descargar). **Nada de lo siguiente ha sido compilado ni ejecutado todavía.**
> Sigue los pasos de "Verificación local" y pega aquí cualquier error para corregirlo.

## Estructura

```
backend/
├── api-gateway/        Spring Cloud Gateway puerto 8080
├── auth-service/       puerto 8081, BD auth_db
├── catalog-service/    puerto 8082, BD catalog_db
├── sales-service/      puerto 8083, BD sales_db
├── docker/mysql-init/  Crea las 3 bases de datos
├── docker-compose.yml  MySQL 8 para desarrollo
├── scripts/run-service.ps1
├── .env.example
└── docs/ARQUITECTURA.md
```

## Requisitos

- JDK 21 (comprobar con `java -version`)
- MySQL 8 (con Docker: `docker compose --env-file .env up -d`) o una instalación local
- No hace falta instalar Maven: cada proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).
  La primera ejecución descarga Maven 3.9.9 y las dependencias (requiere Internet).

## Configuración (Windows / PowerShell)

1. Copia `.env.example` como `.env` y completa:
   - `DB_PASSWORD` y `MYSQL_ROOT_PASSWORD`
   - `JWT_SECRET` (mínimo 32 caracteres, aleatorio). Puedes generarlo con:
     `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])`
2. Levanta MySQL: `docker compose --env-file .env up -d`
   (si usas MySQL local, crea las bases `auth_db`, `catalog_db`, `sales_db` y un usuario `tupolera` con permisos sobre ellas).
3. El archivo `.env` nunca se sube a GitHub (está en `.gitignore`).

## Verificación local

Desde la carpeta `backend`:

```powershell
# 1) Pruebas (no requieren MySQL)
.\scripts\run-service.ps1 auth-service test
.\scripts\run-service.ps1 api-gateway test

# 2) Ejecutar (en dos terminales distintas, auth-service primero)
.\scripts\run-service.ps1 auth-service
.\scripts\run-service.ps1 api-gateway
```

Sin el script, equivalente manual: cargar las variables del `.env` en la sesión y ejecutar
`.\mvnw.cmd test` / `.\mvnw.cmd spring-boot:run` dentro de cada carpeta. En Linux/macOS: `./mvnw test`.

Comprobaciones sugeridas (con ambos servicios levantados):

| Qué | Cómo | Esperado |
|---|---|---|
| Regiones | `GET http://localhost:8080/api/v1/regiones` | 16 regiones |
| Login | `POST http://localhost:8080/api/v1/auth/login` `{"correo":"adminn@gmail.com","password":"Admin#Polera2026!"}` | 200 con `accessToken` y `refreshToken` |
| Sin token | `GET http://localhost:8080/api/v1/usuarios` | 401 |
| Como cliente | mismo GET con el token de `juan@gmail.com` | 403 |
| Como admin | mismo GET con el token del admin | 200 con 3 usuarios |
| Swagger agregado | `http://localhost:8080/swagger-ui.html` | selector con los servicios |
| Swagger de auth | `http://localhost:8081/swagger-ui.html` | UI de auth-service |

Colección Postman inicial: `postman/tupolera-auth.postman_collection.json` (variable `baseUrl` = `http://localhost:8080`).

## Usuarios de prueba (solo perfil `dev`)

| Rol | Correo | Contraseña |
|---|---|---|
| CLIENTE | juan@gmail.com | 123456 |
| VENDEDOR | pedro@gmail.com | 123456 |
| ADMINISTRADOR | adminn@gmail.com | Admin#Polera2026! |

Se cargan desde `auth-service/src/main/resources/db/data-dev-usuarios.sql` únicamente con el perfil `dev`
(el perfil por defecto). Con `SPRING_PROFILES_ACTIVE=prod` no se cargan. Todas las contraseñas están en BCrypt.
En producción, crea el primer administrador manualmente (por ejemplo, con un `INSERT` usando un hash BCrypt propio).

## Endpoints implementados (vía Gateway, `http://localhost:8080`)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/v1/auth/register` | Público (crea CLIENTE) |
| POST | `/api/v1/auth/login` | Público |
| POST | `/api/v1/auth/refresh` | Público (con refresh token) |
| POST | `/api/v1/auth/logout` | Público (con refresh token) |
| GET | `/api/v1/auth/me` | Autenticado |
| GET | `/api/v1/regiones`, `/api/v1/regiones/{id}/comunas` | Público |
| GET/POST/PUT/DELETE | `/api/v1/usuarios[/{id}]` | Solo ADMINISTRADOR (DELETE = desactivación lógica) |

Errores: todos usan el formato `{timestamp, status, error, message, path, detalles?}`.

## Variables de entorno

Ver `.env.example`. `JWT_SECRET` debe ser idéntico en todos los servicios que verifican tokens.

## Pendiente

`catalog-service`, `sales-service`, colección Postman completa, Dockerfiles y guía de despliegue en AWS
(ver `docs/ARQUITECTURA.md`).
