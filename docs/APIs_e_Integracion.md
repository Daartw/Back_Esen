# Documento de APIs e Integración — ESEN Tienda

Describe cómo se comunican el frontend ([Front_Esen](https://github.com/Daartw/Front_Esen), React + Vite) y el backend ([Back_Esen](https://github.com/Daartw/Back_Esen), Spring Boot) mediante REST. El detalle de cada endpoint está en [Documentacion_APIs.md](Documentacion_APIs.md) y en Swagger.

## 1. Arquitectura

```
Navegador (React)                         Spring Boot (API REST)                 MySQL
┌──────────────────────┐   HTTP + JSON   ┌──────────────────────────────┐  JPA  ┌─────────┐
│ páginas y componentes│ ──────────────► │ JwtAuthFilter → SecurityConfig│ ────► │ esen_db │
│ AuthContext/Carrito  │ ◄────────────── │ Controller → Service → Repo   │ ◄──── │         │
│ src/api (Axios)      │  Bearer token   │ ManejadorGlobalExcepciones    │       └─────────┘
└──────────────────────┘                 └──────────────────────────────┘
```

| Capa | Frontend | Backend |
|---|---|---|
| Presentación | `pages/`, `components/` | Swagger UI |
| Estado / sesión | `AuthContext`, `CarritoContext`, `localStorage` | Sin estado (STATELESS) |
| Comunicación | `api/cliente.js` (Axios + interceptores), `api/servicios.js` | `controller/` (REST `/api/v1`) |
| Reglas | validaciones de formulario (`utils/validaciones.js`) | `service/` + Bean Validation |
| Datos | — | `repository/` (Spring Data JPA) → MySQL |

## 2. Configuración de la conexión

| Lado | Variable | Valor local | Producción |
|---|---|---|---|
| Frontend | `VITE_API_URL` | `http://localhost:8080/api/v1` | URL pública del backend + `/api/v1` (variable de entorno en Vercel) |
| Backend | `CORS_ORIGINS` | `http://localhost:*` | `https://*.vercel.app` o el dominio del frontend |
| Backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL local (XAMPP) | Base MySQL del servicio de despliegue |
| Backend | `JWT_SECRET` | clave de desarrollo | clave propia de 32+ caracteres |

CORS se configura en `SecurityConfig.corsConfigurationSource()`: permite los orígenes de `CORS_ORIGINS`, los métodos GET/POST/PUT/PATCH/DELETE/OPTIONS y las cabeceras `Authorization`, `Content-Type` y `Accept`.

## 3. Cliente HTTP del frontend

Todo pasa por `src/api`:

- **`cliente.js`**: instancia única de Axios con `baseURL = VITE_API_URL`.
  - Interceptor de **salida**: agrega `Authorization: Bearer <accessToken>` a cada petición.
  - Interceptor de **entrada**: ante un `401` pide tokens nuevos a `/auth/refresh` **una sola vez** (las peticiones que fallan a la vez esperan la misma renovación) y repite la petición original. Si el refresh también falla, borra la sesión y emite el evento `esen:sesion-expirada`.
- **`servicios.js`**: una función por endpoint (`productosApi.listar()`, `boletasApi.crear()`, `usuariosApi.actualizar()`, etc.).
- **`sesion.js`**: guarda tokens y usuario en `localStorage` y revisa la expiración del JWT.

## 4. Operaciones CRUD por pantalla

| Pantalla | Operación | Endpoint |
|---|---|---|
| Tienda (catálogo y filtros) | Leer | `GET /productos?categoriaId=&q=`, `GET /categorias` |
| Detalle de producto | Leer | `GET /productos/{id}` |
| Registro / Ingresar | Crear sesión | `POST /auth/registro`, `POST /auth/login` |
| Checkout | **Crear** | `POST /boletas` |
| Mis compras | Leer | `GET /boletas/mias` |
| Panel › Productos | Leer, **crear, actualizar, eliminar** | `GET/POST/PUT/DELETE /productos` |
| Panel › Categorías | Leer, **crear, actualizar, eliminar** | `GET/POST/PUT/DELETE /categorias` |
| Panel › Usuarios | Leer, **crear, actualizar, eliminar** | `GET/POST/PUT/DELETE /usuarios` |
| Panel › Órdenes | Leer, **actualizar estado, eliminar** | `GET /boletas`, `PUT /boletas/{id}/estado`, `DELETE /boletas/{id}` |
| Panel › Resumen | Leer | `GET /boletas`, `GET /productos`, `GET /usuarios` |

## 5. Flujos de integración

### 5.1 Inicio de sesión y sesión persistente

1. `Login` valida email y contraseña en JS y llama `POST /auth/login`.
2. El backend valida con BCrypt y responde `accessToken` (30 min), `refreshToken` (7 días) y el usuario con su rol.
3. `AuthContext` guarda todo en `localStorage` y redirige según el rol (`rutaInicial`).
4. **Al recargar la página**, `AuthContext` lee la sesión guardada, renueva el token si venció y consulta `GET /auth/me` para confirmar el usuario y el rol con el backend. Mientras tanto, las rutas protegidas muestran "Cargando…" en vez de expulsar al usuario.

### 5.2 Compra

1. El carrito vive en el navegador (`CarritoContext` + `localStorage`).
2. En Checkout se valida la dirección y se envía `POST /boletas` **solo con `productoId` y `cantidad`**.
3. El backend, en una transacción, bloquea cada producto (`SELECT … FOR UPDATE`), valida stock y estado, toma el precio de la base, calcula neto/IVA/total, descuenta stock y guarda la boleta.
4. Si un producto falla, se revierte todo y el frontend muestra el mensaje del backend (`mensajeDeError`).
5. Si sale bien, el frontend muestra la boleta y vacía el carrito.

### 5.3 Mantenedores del panel

1. El formulario valida en JS (mismas reglas que el backend) y muestra el error junto a cada campo.
2. Se envía `POST` (crear) o `PUT` (actualizar). Si el backend responde `400` con `campos`, o `409` (por ejemplo email repetido), el mensaje se muestra en el formulario.
3. Tras guardar o eliminar se vuelve a pedir la lista (`recargar()`).

## 6. Seguridad en la integración

| Rol | Tienda | Panel |
|---|---|---|
| Visitante | Catálogo y carrito | — |
| Cliente | Catálogo, compra y mis compras | — |
| Vendedor | — (va al panel) | Productos y órdenes en solo lectura |
| Administrador | Todo | Todo |

Las restricciones se aplican en tres niveles: rutas del frontend (`RutaProtegida`), interfaz (solo se dibujan los botones permitidos) y backend (`SecurityConfig`, que responde 401/403 aunque alguien fuerce la petición). El JWT se firma con HS256, se valida en `JwtAuthFilter` (firma, expiración y tipo de token) y el rol se carga desde la base en cada petición.

## 7. Manejo de errores entre capas

El backend responde siempre con `ErrorResponse` (`estado`, `mensaje`, `campos`). En el frontend, `mensajeDeError()` lo traduce a un texto para el usuario:

| Situación | Lo que ve el usuario |
|---|---|
| Backend apagado o sin red | "No se pudo conectar con el servidor…" |
| `400` con `campos` | Los mensajes de cada campo |
| `401` | Renovación automática del token; si no se puede, vuelve al login |
| `403` | Página "Sin acceso" o mensaje de permisos |
| `409` | El mensaje del conflicto (ej. "Ya existe una cuenta con ese email") |

## 8. Pruebas de la integración

- **Backend**: `ApiIntegracionTest` (MockMvc + H2) recorre login, permisos por rol, CRUD y compra completa. `BoletaServiceTest` prueba la lógica de venta (totales, IVA, stock).
- **Frontend**: pruebas con Jasmine y Karma de validaciones, sesión, carrito y componentes (ver `docs/Cobertura_Testing.md` en Front_Esen).
- **Manual**: Swagger UI para probar cada endpoint con un token.
