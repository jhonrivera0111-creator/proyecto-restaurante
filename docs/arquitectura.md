# Arquitectura — Casa Verde

Documento de referencia de la arquitectura del sistema. Para instalar y ejecutar, ver el [README](../README.md). El contrato completo de cada endpoint está en [CLAUDE.md](../CLAUDE.md#api-rest--endpoints-implementados) y en Swagger UI (`http://localhost:5020/swagger-ui.html` con el backend corriendo).

## 1. Vista general

```
┌──────────────────────────┐      HTTP/JSON       ┌───────────────────────────────┐
│  Frontend (React SPA)    │  ─────────────────▶  │  Backend (Spring Boot :5020)  │
│  Vite :5173 (dev)        │     /api/v1/...      │  API REST + reglas de negocio │
│  nginx :80 (Docker)      │  ◀─────────────────  │                               │
│  Electron (escritorio)   │   Bearer token       └───────────────┬───────────────┘
└──────────────────────────┘                                      │ JPA / Hibernate
                                                                  ▼
                                                   ┌───────────────────────────────┐
                                                   │ SQLite: DataBase/casaverde.db │
                                                   │ Esquema versionado con Flyway │
                                                   └───────────────────────────────┘
```

- **Cliente delgado:** el frontend solo presenta datos y captura entradas. Todos los cálculos de dinero, estados e inventario se hacen en el backend.
- **Ejecución local:** una sola máquina, sin dependencias de red externas. La base de datos es un archivo SQLite; el diseño (JPA + Flyway) permite migrar a PostgreSQL sin cambiar la lógica.
- **Tres formas de desplegar el mismo código:** desarrollo (Vite + `spring-boot:run`), Docker Compose (nginx + JAR) e instalador Electron (JAR + JRE embebidos).

## 2. Backend — arquitectura en capas

Paquete raíz: `Backend/src/main/java/com/casaverde/backend/`

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Presentación | `capaControladores/` | `@RestController`: recibe la petición HTTP, valida el formato (`@Valid`) y delega al servicio. Sin lógica de negocio. |
| Fachada de servicios | `fachadaServices/services/*/IXxxService` | Interfaces que definen las operaciones del dominio. Los controladores dependen solo de ellas. |
| Lógica de negocio | `fachadaServices/services/*/XxxServiceImpl` | Reglas del negocio, transacciones (`@Transactional`) y auditoría. |
| DTOs y mapeo | `fachadaServices/dto/`, `fachadaServices/mapper/` | Objetos de entrada/salida de la API; conversión entidad ↔ DTO con MapStruct. |
| Acceso a datos | `capaAccesoADatos/repositories/` | Interfaces Spring Data JPA. |
| Modelo | `capaAccesoADatos/models/entitys/`, `enums/` | Entidades JPA y enums del dominio. |
| Transversal | `config/` | Seguridad, CORS, datos iniciales, manejo global de errores, dialecto SQLite, OpenAPI. |

Flujo de una petición:

```
HTTP → Controller → IXxxService → XxxServiceImpl → Repository → Entity → SQLite
                                        │
                                        ├─ CajaMovimiento (libro de caja)
                                        ├─ MovimientoInventario
                                        └─ HistorialAuditoria
```

### Decisiones de diseño

| Decisión | Motivo |
|---|---|
| `BigDecimal` para todo dinero e inventario | Evitar errores de redondeo de `double` en caja (ej. 1/8 de gallina = 0,125). |
| `@Version` (bloqueo optimista) en Pedido, Pago, Fondo, Inventario, Producto… | Dos usuarios cobrando o descontando stock a la vez no pueden pisarse los saldos. |
| Snapshots en `PedidoDetalle` (nombre, precio, composición) | Si luego cambia el precio o la receta de un producto, los pedidos históricos no se alteran. |
| Fondos separados por moneda (COP, USD, NEQUI y cuentas NEQUI adicionales) | En la operación real son cajas físicas/cuentas distintas y se cuadran por separado. |
| Libro de caja inmutable (`CajaMovimiento` con `saldoResultante`) | Toda variación de un fondo queda trazada con su origen (`referenciaTipo` + `referenciaId`). Las correcciones se hacen con movimientos de reverso, no borrando. |
| Flyway (`db/migration/V1..V8`) + `ddl-auto=validate` | El esquema evoluciona con migraciones versionadas y reproducibles. |
| Zona horaria fija `America/Bogota` | Las fechas de cierre y reportes deben coincidir con el día operativo del negocio. |

### Seguridad

- Login (`POST /api/v1/sesion/login`) valida la contraseña con **BCrypt** y devuelve un **token firmado** con vigencia de 12 h.
- El frontend envía el token en `Authorization: Bearer ...` en cada petición.
- Roles: **ADMIN** (requiere contraseña; acceso a usuarios, configuración y backups) y **OPERADOR** (operación diaria).
- Rutas públicas: login, listado de usuarios (para la pantalla de inicio), `/actuator/health` y Swagger.

## 3. Frontend

Carpeta `Frontend/src/`:

| Carpeta | Contenido |
|---|---|
| `pages/` | Una carpeta por pantalla: `dashboard`, `pedidos`, `fondos`, `inventario`, `resumen`, `empleados`, `cuentas-cobrar`, `configuracion` |
| `components/` | Modales y componentes por dominio; `shared/` para los reutilizables |
| `services/` | Un cliente Axios por recurso de la API (`api.js` agrega el token) |
| `hooks/` | Mutaciones de TanStack Query y utilidades de UI |
| `store/` | Zustand: solo la sesión del usuario y el estado del sidebar |
| `utils/` | Formato de moneda/fechas y constantes de dominio |

- **Datos del servidor:** TanStack Query (caché e invalidación tras cada mutación). No se guardan datos de la API en el estado global.
- **Formularios:** react-hook-form + zod, solo validación de formato; la validación de negocio vive en el backend.

## 4. Modelo de dominio

| Agregado | Entidades | Descripción |
|---|---|---|
| Pedidos | `Pedido`, `PedidoDetalle`, `Pago` | Pedido por mesa/salón o para llevar, ítems y pagos en varias monedas |
| Catálogo | `Producto`, `ProductoComposicionInventario`, `ProductoPrecioHistorial`, `CategoriaProducto`, `Salon` | Productos con receta de insumos e historial de precios |
| Caja | `Fondo`, `CajaMovimiento`, `Gasto`, `TasaCambio`, `CierreCaja` | Saldos por moneda, libro de caja, salidas y cierre diario |
| Inventario y compras | `Inventario`, `MovimientoInventario`, `Proveedor`, `Compra`, `AbonoCompra`, `AdelantoProveedor` | Stock de insumos y deuda con proveedores |
| Personal | `Empleado`, `Adelanto`, `TrabajoDia`, `PagoEmpleado`, `Vacacion` | Nómina quincenal o por día |
| Cartera | `CuentaCobrar`, `AbonoCuentaCobrar` | Créditos a clientes |
| Sistema | `Usuario`, `ConfiguracionGeneral`, `HistorialAuditoria` | Usuarios, parámetros y auditoría |

## 5. Reglas de negocio principales

### Ciclo de vida del pedido

```
PENDIENTE ──pago parcial──▶ ABONADO_PARCIAL ──saldo = 0──▶ PAGADO ──finalizar──▶ FINALIZADO
    │                            │                            │
    └────────────────────────────┴────────── anular ──────────┴──────────────▶ ANULADO
```

1. **Registrar un pago no mueve la caja**: solo cambia el estado del pedido.
2. **Finalizar** (solo si está `PAGADO`), en una única transacción:
   - valida que haya stock suficiente y descuenta el inventario según la composición guardada en cada ítem;
   - acredita cada fondo por la moneda en que se pagó (`COBRO_PEDIDO` en el libro de caja);
   - registra la auditoría.
3. **Anular** solo es posible antes de finalizar; como la caja nunca se acreditó, no hay que revertir dinero.

### Caja, inventario y cierre

- Un **gasto** descuenta el fondo (valida saldo suficiente) y, si es un insumo, suma stock.
- Una **compra a proveedor** suma stock al registrarse; los **abonos** son los que debitan el fondo.
- El **cobro de una cuenta por cobrar** acredita el fondo; registrar la deuda no lo toca.
- El **cierre de caja** es único por fecha, compara saldo esperado vs. contado, se bloquea si hay pedidos activos y genera un backup de la base de datos.
- Se genera además un **backup automático diario** a las 00:00 (y uno al arrancar si no existe el del día).

## 6. Pruebas

| Nivel | Herramienta | Ubicación |
|---|---|---|
| Unitarias y de integración del backend (servicios + BD SQLite temporal) | JUnit 5, Spring Boot Test | `Backend/src/test/java/` |
| Unitarias y de componentes del frontend | Vitest, Testing Library (jsdom) | `Frontend/src/**/__tests__/` |

Cómo ejecutarlas: ver [README → Pruebas automatizadas](../README.md#pruebas-automatizadas).
