# Documentación de APIs — ESEN Tienda

API REST de la tienda ESEN, construida con Spring Boot 3.3 y documentada con OpenAPI (springdoc).

| Dato | Valor |
|---|---|
| URL base local | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Especificación OpenAPI (JSON) | `http://localhost:8080/v3/api-docs` |
| Formato | JSON (`Content-Type: application/json`) |
| Autenticación | JWT Bearer: `Authorization: Bearer <accessToken>` |
| Versionado | En la ruta (`/api/v1`) |

En Swagger, el botón **Authorize** permite pegar el access token obtenido en `/auth/login` para probar los endpoints protegidos.

## 1. Autenticación (`/auth`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público | Valida email y contraseña; entrega access token (30 min) y refresh token (7 días). |
| POST | `/auth/registro` | Público | Crea una cuenta con rol CLIENTE y deja la sesión iniciada. |
| POST | `/auth/refresh` | Público | Entrega un par de tokens nuevo a partir de un refresh token válido. |
| GET | `/auth/me` | Autenticado | Devuelve el usuario dueño del token (se usa al recargar la página). |

**Ejemplo — login**

```http
POST /api/v1/auth/login
{ "email": "cliente@esen.cl", "password": "Cliente123*" }
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 1800,
  "usuario": { "id": 3, "nombre": "Camila", "apellido": "Soto", "email": "cliente@esen.cl", "rol": "CLIENTE", "direccion": "...", "activo": true, "fechaCreacion": "2026-11-21T10:00:00" }
}
```

**Registro** — cuerpo: `nombre`*, `apellido`*, `email`*, `password`* (8 a 72), `direccion` (opcional).
**Refresh** — cuerpo: `{ "refreshToken": "..." }`.

## 2. Productos (`/productos`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/productos` | Público | Lista productos activos. Filtros: `categoriaId`, `q` (texto en el nombre), `incluirInactivos` (panel). |
| GET | `/productos/{id}` | Público | Detalle de un producto. |
| POST | `/productos` | ADMIN | Crea un producto. |
| PUT | `/productos/{id}` | ADMIN | Actualiza un producto. |
| DELETE | `/productos/{id}` | ADMIN | Elimina el producto; si tiene ventas, solo lo desactiva. |

Cuerpo (POST/PUT):

| Campo | Tipo | Reglas |
|---|---|---|
| `nombre` | texto | obligatorio, máx. 120 |
| `descripcion` | texto | máx. 1000 |
| `precio` | entero (CLP, IVA incluido) | obligatorio, ≥ 1 |
| `stock` | entero | obligatorio, ≥ 0 |
| `color` | texto | máx. 40 |
| `imagenUrl` | texto | máx. 500 |
| `categoriaId` | número | obligatorio, debe existir |
| `activo` | booleano | visible en la tienda |

Respuesta: `id, nombre, descripcion, precio, stock, color, imagenUrl, activo, categoriaId, categoriaNombre`.

## 3. Categorías (`/categorias`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/categorias` | Público | Lista las categorías. |
| GET | `/categorias/{id}` | Público | Obtiene una categoría. |
| POST | `/categorias` | ADMIN | Crea una categoría (`nombre`* máx. 60, `descripcion` máx. 255). |
| PUT | `/categorias/{id}` | ADMIN | Actualiza una categoría. |
| DELETE | `/categorias/{id}` | ADMIN | Elimina una categoría **sin productos** (si tiene, responde 409). |

## 4. Usuarios (`/usuarios`) — solo ADMIN

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/usuarios` | Lista los usuarios. |
| GET | `/usuarios/{id}` | Obtiene un usuario. |
| POST | `/usuarios` | Crea un usuario con cualquier rol. |
| PUT | `/usuarios/{id}` | Actualiza; si `password` viene vacía se conserva la actual. Un admin no puede quitarse su propio rol. |
| DELETE | `/usuarios/{id}` | Elimina; si tiene boletas solo lo desactiva. Un admin no puede eliminarse a sí mismo. |

Cuerpo: `nombre`*, `apellido`*, `email`*, `password` (obligatoria al crear), `rol`* (`ADMIN`, `VENDEDOR`, `CLIENTE`), `direccion`, `activo`.

## 5. Boletas (`/boletas`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/boletas` | CLIENTE, ADMIN | Registra una compra: valida stock, calcula neto, IVA y total, y descuenta stock. |
| GET | `/boletas/mias` | Autenticado | Compras del usuario del token. |
| GET | `/boletas` | ADMIN, VENDEDOR | Todas las boletas. |
| GET | `/boletas/{id}` | ADMIN, VENDEDOR | Una boleta con su detalle. |
| PUT | `/boletas/{id}/estado` | ADMIN | Cambia el estado (`PAGADA`, `ENVIADA`, `ENTREGADA`, `ANULADA`); al anular repone el stock. |
| DELETE | `/boletas/{id}` | ADMIN | Elimina una boleta **anulada**. |

**Ejemplo — compra**

```http
POST /api/v1/boletas
Authorization: Bearer <accessToken>
{
  "direccionEnvio": "Av. Providencia 1234, Providencia",
  "items": [ { "productoId": 1, "cantidad": 2 }, { "productoId": 5, "cantidad": 1 } ]
}
```

El frontend solo envía `productoId` y `cantidad` (1 a 20). Precio, neto, IVA (19 %) y total los calcula el backend con los datos de la base. Respuesta: `id, numero, fecha, estado, clienteId, clienteNombre, clienteEmail, direccionEnvio, neto, iva, total, detalles[]` (cada detalle: `productoId, productoNombre, cantidad, precioUnitario, subtotal`).

## 6. Estado

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/health` | Público | Indica si el API está disponible. |

## 7. Códigos de respuesta y errores

Todos los errores usan el mismo formato (`ErrorResponse`):

```json
{
  "fecha": "2026-11-21T10:15:00",
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "Datos inválidos",
  "ruta": "/api/v1/productos",
  "campos": { "precio": "El precio debe ser mayor a 0" }
}
```

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | Operación correcta (consulta / creación / eliminación). |
| 400 | Datos inválidos (con `campos`), JSON mal formado o regla de negocio (ej. stock insuficiente). |
| 401 | Sin token, token vencido o inválido, o credenciales incorrectas. |
| 403 | El rol del usuario no tiene permiso para la acción. |
| 404 | El recurso o la ruta no existe. |
| 405 | Método HTTP no permitido en la ruta. |
| 409 | Conflicto: email o categoría repetidos, categoría con productos, etc. |
| 500 | Error interno. |

## 8. Usuarios de demostración

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | admin@esen.cl | `Admin123*` |
| Vendedor | vendedor@esen.cl | `Vendedor123*` |
| Cliente | cliente@esen.cl | `Cliente123*` |
