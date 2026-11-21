# ESEN Tienda — Backend

API REST de la tienda online **ESEN** (ropa básica en tonos neutros). Proyecto académico de
DSY1104 Desarrollo Fullstack II, Evaluación Parcial 3. El frontend está en
[Front_Esen](https://github.com/Daartw/Front_Esen).

> Proyecto con fines educativos. No es una tienda real ni está afiliado a ninguna marca.

## Tecnologías

| Capa | Herramienta |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation) |
| Base de datos | MySQL 8 (H2 en memoria solo para las pruebas) |
| Autenticación | JWT (jjwt 0.12) con access token y refresh token |
| Documentación | Swagger / OpenAPI (springdoc) |
| Pruebas | JUnit 5, Mockito, MockMvc |

## Cómo ejecutarlo en local

Requisitos: JDK 17 o superior, Maven 3.9 y MySQL corriendo (por ejemplo con XAMPP).

```bash
git clone https://github.com/Daartw/Back_Esen.git
cd Back_Esen
mvn spring-boot:run
```

Con la configuración por defecto se conecta a `localhost:3306` con el usuario `root` sin clave y
crea sola la base `esen_db`, sus tablas y los datos de demostración.

- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html

### Variables de entorno

| Variable | Para qué sirve | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/esen_db...` |
| `DB_USERNAME` | Usuario de la base | `root` |
| `DB_PASSWORD` | Clave de la base | vacío |
| `JWT_SECRET` | Clave de firma de los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `CORS_ORIGINS` | Orígenes del frontend, separados por coma | `http://localhost:*,https://*.vercel.app` |
| `PORT` | Puerto HTTP | `8080` |
| `SEED_ENABLED` | Carga los datos de demostración si la base está vacía | `true` |

### Usuarios de demostración

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | admin@esen.cl | `Admin123*` |
| Vendedor | vendedor@esen.cl | `Vendedor123*` |
| Cliente | cliente@esen.cl | `Cliente123*` |

En un despliegue público conviene cambiarlas con `SEED_ADMIN_PASSWORD`, `SEED_VENDEDOR_PASSWORD`
y `SEED_CLIENTE_PASSWORD` antes del primer arranque.

## Modelo de datos

```
USUARIO 1 ──── N BOLETA 1 ──── N DETALLE_BOLETA N ──── 1 PRODUCTO N ──── 1 CATEGORIA
```

- **usuario**: datos de la cuenta, contraseña cifrada con BCrypt y rol (`ADMIN`, `VENDEDOR`, `CLIENTE`).
- **boleta**: una compra; guarda folio, fecha, estado, dirección, neto, IVA y total.
- **detalle_boleta**: cada línea de la compra, con la cantidad y el precio al momento de comprar.
- **producto** y **categoria**: el catálogo.

## Reglas de negocio

- El precio y el total los calcula el backend con los datos de la base; el frontend solo envía
  producto y cantidad.
- No se vende sin stock ni productos inactivos. La compra es una transacción: si un producto falla,
  no se descuenta nada.
- La fila del producto se bloquea durante la venta para que dos compras simultáneas no vendan la
  misma unidad.
- Los precios incluyen IVA (19 %); la boleta guarda el desglose neto e IVA.
- Al anular una boleta se repone el stock. Una boleta entregada no se puede anular y solo se
  eliminan boletas anuladas.
- Un producto con ventas o un usuario con compras no se borran: se desactivan.
- No se puede eliminar una categoría con productos, ni repetir email o nombre de categoría.
- Un administrador no puede eliminarse ni quitarse su propio rol.

## Endpoints (`/api/v1`)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/login`, `/auth/registro`, `/auth/refresh` | Público |
| GET | `/auth/me` | Autenticado |
| GET | `/productos`, `/productos/{id}` | Público |
| POST, PUT, DELETE | `/productos`, `/productos/{id}` | ADMIN |
| GET | `/categorias`, `/categorias/{id}` | Público |
| POST, PUT, DELETE | `/categorias`, `/categorias/{id}` | ADMIN |
| GET, POST, PUT, DELETE | `/usuarios`, `/usuarios/{id}` | ADMIN |
| POST | `/boletas` | CLIENTE, ADMIN |
| GET | `/boletas/mias` | Autenticado |
| GET | `/boletas`, `/boletas/{id}` | ADMIN, VENDEDOR |
| PUT | `/boletas/{id}/estado` | ADMIN |
| DELETE | `/boletas/{id}` | ADMIN |

El detalle de cada uno (datos de entrada y respuestas) está en Swagger.

## Seguridad

1. `POST /auth/login` valida email y contraseña (BCrypt) y entrega un **access token** de 30 minutos
   y un **refresh token** de 7 días, ambos firmados con HS256.
2. El frontend envía `Authorization: Bearer <accessToken>` en cada petición.
3. `JwtAuthFilter` valida firma, expiración y tipo de token, y carga el rol vigente desde la base.
4. `SecurityConfig` decide qué rol puede usar cada ruta. Sin token responde **401**; con un rol sin
   permiso responde **403**.
5. Cuando el access token vence, `POST /auth/refresh` entrega un par nuevo sin pedir la clave otra vez.

## Documentación

- [Documentación de APIs](docs/Documentacion_APIs.md): endpoints, cuerpos, respuestas y códigos de error.
- [APIs e Integración](docs/APIs_e_Integracion.md): cómo se comunica el frontend con este API, flujos y seguridad.
- ERS, manual de usuario y cobertura de pruebas del frontend: carpeta `docs/` de [Front_Esen](https://github.com/Daartw/Front_Esen/tree/main/docs).

## Pruebas

```bash
mvn test
```

- `BoletaServiceTest`: pruebas unitarias de la lógica de venta (totales, IVA, stock).
- `ApiIntegracionTest`: pruebas de integración del API completo (login, permisos por rol, CRUD, compra).

## Despliegue

El repositorio incluye un `Dockerfile`. En Render o Railway: crear un servicio web desde este
repositorio, agregar una base MySQL y definir `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
y `CORS_ORIGINS` con la URL del frontend en Vercel.

## Estructura

```
src/main/java/cl/esen/tienda
├── config       Seguridad, CORS, Swagger y datos iniciales
├── controller   Controladores REST (/api/v1)
├── dto          Objetos de entrada y salida del API
├── exception    Excepciones y manejador global de errores
├── model        Entidades JPA
├── repository   Repositorios Spring Data
├── security     Servicio JWT y filtro de autenticación
└── service      Lógica de negocio
```
