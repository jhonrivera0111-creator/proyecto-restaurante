# AGENTS.md

## Propósito

Este repositorio implementa un sistema de escritorio para la operación de un asadero/restaurante con foco en rapidez operativa, trazabilidad financiera, control de inventario y ejecución local.

Codex debe comportarse como:

- arquitecto de software senior,
- analista funcional,
- desarrollador full-stack senior,
- revisor de calidad técnica.

La prioridad no es una demo visual; la prioridad es un sistema operable de verdad, consistente, mantenible y preparado para crecer de monousuario local a esquema cliente-servidor.

---

## Stack

### Backend
| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 17 + Spring Boot 3.4.7 |
| ORM / Persistencia | Spring Data JPA + Hibernate + SQLiteDialect custom |
| Base de datos | SQLite (local) |
| Mapeo DTO | MapStruct 1.6.3 |
| Boilerplate | Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Data`, etc.) |
| Seguridad / hashing | Spring Security (filtro de token propio firmado HMAC-SHA256, roles ADMIN/OPERADOR) + BCrypt para contraseñas |
| Puerto local | `5020` |

### Frontend
| Capa | Tecnología |
|---|---|
| UI | React 19 + Vite 7 |
| Estilos | Tailwind CSS v4 (clases utilitarias + `@layer components` propias) |
| Routing | React Router v7 (`createBrowserRouter`) |
| Estado global | Zustand v5 (`useSessionStore`) |
| Fetching / caché | TanStack React Query v5 |
| HTTP | Axios — base URL `/api/v1` (relativa; nginx la proxea al backend) |
| Formularios | react-hook-form + zod |
| Iconos | lucide-react |
| Fechas | date-fns |
| Tests | Vitest + Testing Library (jsdom) |
| Desktop | Electron 42.3.0 + electron-builder 26.0.12 — carpeta `electron/` |

### Despliegue (Docker Compose)
- `casaverde-backend` → `:5020` (Spring Boot JAR)
- `casaverde-frontend` → `:80` (nginx sirve React SPA + proxy `/api/*` al backend)
- Frontend usa URLs relativas `/api/v1/...`; nginx hace el proxy. No hay CORS en producción.
- En desarrollo (`npm run dev`): Vite corre en `:5173`, llamadas a `/api/v1` van directo a `:5020` (CORS habilitado para ese origen).

### Decisiones base

- La lógica de negocio vive en backend; el frontend es delgado y consume la API.
- Los repositories NO llevan `@Repository` — Spring Data los registra automáticamente por extender `JpaRepository`.
- Los servicios se inyectan siempre por su **interfaz** (`IXxxService`), nunca por la clase impl.
- No se usa `@Autowired` — inyección por constructor vía `@RequiredArgsConstructor`.
- `BigDecimal` para todo cálculo monetario. Nunca `double` ni `float`.
- Zona horaria operativa: `America/Bogota` (hardcoded en `@PrePersist`/`@PreUpdate`).

---

## Estructura real del proyecto

```
Casa-Verde/
├── Backend/          ← Spring Boot 3.4 / Java 17
├── Frontend/         ← React 19 + Vite + Tailwind v4
│   └── electron/     ← Electron 42 (main.js, preload.js, splash.html)
├── docker-compose.yml
├── AGENTS.md
└── CLAUDE.md
```

### Backend (`Backend/src/main/java/com/casaverde/backend/`)
```
├── CasaVerdeApplication.java         ← @SpringBootApplication raíz
├── capaAccesoADatos/
│   ├── models/
│   │   ├── entitys/   ← 18 @Entity JPA
│   │   └── enums/     ← 4 enums de dominio
│   └── repositories/  ← 18 interfaces JpaRepository (sin @Repository)
├── capaControladores/ ← 15 @RestController
├── config/
│   ├── DataSourceConfig.java         ← DataSource SQLite
│   ├── CorsConfig.java               ← CORS localhost:5173, :80, null/Electron
│   ├── DataInitializer.java          ← CommandLineRunner — carga datos iniciales
│   ├── controllerAdvice/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ValidacionException.java
│   │   └── RecursoNoEncontradoException.java
│   └── dialect/
│       └── SQLiteDialect.java
└── fachadaServices/
    ├── dto/      ← 41 DTOs y Requests
    ├── mapper/   ← 12 @Mapper MapStruct
    └── services/
        ├── configuracionService/   ← IConfiguracionGeneralService, ITasaCambioService
        ├── pagoService/            ← IFondoService, ICajaMovimientoService
        ├── pedidoService/          ← IPedidoService, ICierreCajaService, IResumenService,
        │                              IComprobanteService
        └── productoService/        ← IProductoService, ICategoriaProductoService,
                                       IInventarioService, ISalonService, IGastoService,
                                       IUsuarioService, IMonetaryService,
                                       IBackupService, IExportacionService, IAuditoriaService
```

---

## API REST — Endpoints implementados

### Pedidos — `/api/v1/pedidos`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista pedidos activos. `?todos=true` devuelve todos |
| GET | `/{id}` | Detalle de un pedido |
| GET | `/{id}/comprobante` | Comprobante texto plano (`text/plain`) |
| POST | `/` | Crear pedido (con detalles y abono inicial opcional) |
| PATCH | `/{id}` | Actualizar datos del pedido (nombre, salón, mesa, etc.) |
| POST | `/{id}/detalles` | Agregar ítem al pedido |
| PATCH | `/{id}/detalles/{detalleId}` | Actualizar cantidad de un ítem |
| DELETE | `/{id}/detalles/{detalleId}` | Quitar ítem del pedido |
| POST | `/{id}/pagos` | Registrar pago (COP / USD / NEQUI) |
| PATCH | `/{id}/pagos/{pagoId}` | Editar un pago |
| DELETE | `/{id}/pagos/{pagoId}` | Quitar un pago |
| PUT | `/{id}/finalizar` | Finalizar pedido (requiere saldo = 0) |
| PUT | `/{id}/anular` | Anular pedido |

### Productos — `/api/v1/productos`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista productos. `?soloActivos=true` filtra inactivos |
| GET | `/{id}` | Detalle de un producto |
| GET | `/{id}/precios` | Historial de cambios de precio |
| POST | `/` | Crear producto con composición de inventario |
| PUT | `/{id}` | Actualizar producto |
| PUT | `/{id}/precio` | Actualizar precio (registra historial automático) |
| PUT | `/{id}/toggle` | Activar / desactivar |

### Categorías — `/api/v1/categorias`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista categorías. `?soloActivas=true` |
| GET | `/{id}` | Detalle |
| POST | `/` | Crear |
| PUT | `/{id}` | Actualizar |
| PUT | `/{id}/toggle` | Activar / desactivar |

### Inventario — `/api/v1/inventario`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Stock actual de todos los insumos |
| GET | `/{insumo}` | Stock de un insumo específico |
| GET | `/movimientos` | Historial de movimientos. `?insumo=gallina` filtra |
| POST | `/movimiento` | Registrar entrada o ajuste manual |

### Gastos — `/api/v1/gastos`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista gastos. `?fecha=2025-01-15` filtra por día |
| POST | `/` | Registrar gasto (descuenta fondo, puede sumar inventario) |
| DELETE | `/{id}` | Eliminar gasto |

### Fondos — `/api/v1/fondos`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Saldo de todos los fondos (COP, USD, NEQUI y cuentas NEQUI adicionales) |
| GET | `/historial` | Historial libro de caja. Filtros: `fecha`, `moneda`, `tipo` |
| PUT | `/{moneda}` | Ajustar saldo de un fondo manualmente |
| POST | `/entrada` | Registrar entrada directa de capital a un fondo |
| POST | `/transferencia` | Transferir saldo entre fondos (genera par TRANSFERENCIA_SALIDA/ENTRADA) |
| GET | `/nequi/estado-mes` | Resumen de operaciones NEQUI del mes actual |
| POST | `/nequi/cuentas` | Crear una cuenta NEQUI adicional |
| DELETE | `/nequi/cuentas/{moneda}` | Eliminar una cuenta NEQUI adicional |

### Tasa de cambio — `/api/v1/tasa-cambio`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/vigente` | Tasa USD → COP vigente |
| GET | `/historial` | Historial de tasas |
| POST | `/` | Registrar nueva tasa (`{valorCop}`) |

### Configuración — `/api/v1/configuracion`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Todos los parámetros clave-valor |
| GET | `/{clave}` | Un parámetro |
| POST | `/` | Crear parámetro |
| PUT | `/{clave}` | Actualizar valor |
| DELETE | `/{clave}` | Eliminar parámetro |

### Salones — `/api/v1/salones`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista salones. `?soloActivos=true` |
| GET | `/{id}` | Detalle |
| POST | `/` | Crear |
| PUT | `/{id}` | Actualizar |
| PUT | `/{id}/toggle` | Activar / desactivar |

### Cierre de caja — `/api/v1/cierres-caja`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Historial de cierres |
| GET | `/{fecha}` | Cierre de una fecha (yyyy-MM-dd) |
| POST | `/` | Cerrar caja del día (requiere 0 pedidos abiertos, crea backup) |

### Resumen del día — `/api/v1/resumen`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Resumen completo. `?fecha=2025-01-15` (default: hoy) |

### Usuarios — `/api/v1/usuarios`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista usuarios activos |
| POST | `/` | Crear operador (`{nombre, password}`) |
| PUT | `/{id}/toggle` | Activar / desactivar usuario |
| PUT | `/{id}/password` | Cambiar contraseña |
| DELETE | `/{id}` | Eliminar usuario (solo OPERADOR) |

### Sesión — `/api/v1/sesion`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/login` | Login (`{usuarioId, password}`) — devuelve UsuarioDTO con `token`; clave incorrecta → 400 |

### Auditoría — `/api/v1/auditoria`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Historial. Filtros opcionales: `entidad`, `entidadId` |

### Backups — `/api/v1/backups`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista backups disponibles |
| POST | `/` | Crear backup manual. `?motivo=manual` |

### Exportaciones — `/api/v1/exportaciones`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/resumen` | CSV resumen del día. `?fecha=` |
| GET | `/caja` | CSV libro de caja del día. `?fecha=` |

---

## Entidades implementadas

### Pedido
- Campos: `id`, `nombre`, `estado`, `mesa`, `salonId`, `fechaReserva`, `horaReserva`, `cantPersonas`, `paraLlevar`, `observacion`
- Totales calculados al vuelo por mapper: `totalCop`, `totalAbonado`, `saldoPendiente`, `valorCambio`
- Relaciones: `@OneToMany detalles` (cascade ALL), `@OneToMany pagos` (cascade ALL)
- `@Version` para bloqueo optimista
- Estados: `PENDIENTE` → `ABONADO_PARCIAL` → `PAGADO` → `FINALIZADO` / `ANULADO`

### PedidoDetalle
- `nombreSnapshot`: copia del nombre del producto al momento de agregar
- `composicionSnapshot`: JSON de la composición inventariable (snapshot, no FK a Producto)
- `precioUnitarioCop`, `cantidad`

### Pago
- `moneda` (COP | USD | NEQUI), `monto`, `montoCop`, `tasaUsada`
- Los pagos **no** afectan el fondo hasta que el pedido se **finaliza**

### Producto
- `afectaInventario` (boolean): si true, al finalizar el pedido se descuenta inventario
- `composicion`: lista de `ProductoComposicionInventario` (insumo + cantidad)
- `@Version` para bloqueo optimista

### ProductoPrecioHistorial
- Se crea automáticamente en `actualizarPrecio()`: guarda precio anterior, precio nuevo y motivo

### Fondo
- PK = `moneda` (string: "COP", "USD", "NEQUI")
- Soporta cuentas NEQUI adicionales con alias (ej. "NEQUI_HAROLD")
- `alias`: nombre legible del fondo
- `@Version` para bloqueo optimista concurrente
- El fondo se acredita **al finalizar un pedido**, no al registrar el pago

### CajaMovimiento
- `tipo`: COBRO_PEDIDO | REVERSO_PAGO | GASTO | REVERSO_GASTO | AJUSTE_FONDO | ENTRADA_MANUAL | TRANSFERENCIA_SALIDA | TRANSFERENCIA_ENTRADA
- `referenciaTipo` + `referenciaId`: trazabilidad al origen
- Cada movimiento guarda `saldoResultante` en el momento del registro

### Gasto
- `categoria`: insumos | servicios | equipos | otros
- `insumoInventario` + `cantidadInventario`: si se completan, suma stock al inventario
- Descuenta el fondo correspondiente en la misma transacción

### Inventario
- Un registro por insumo (`"gallina"`, `"cuy"`)
- `@Version` para bloqueo optimista

### MovimientoInventario
- `tipo`: entrada | salida | ajuste
- `referenciaTipo`: pedido | gasto | ajuste_manual

### TasaCambio
- Historial completo; la vigente es la de mayor `vigenteDesde`
- Valor inicial: 1 USD = 4200 COP

### CierreCaja
- Único por fecha (`UNIQUE(fecha)`)
- Guarda esperado vs contado en los tres fondos y genera backup automático
- Bloqueado si hay pedidos activos (PENDIENTE, ABONADO_PARCIAL, PAGADO)

### Usuario
- `rol`: ADMIN | OPERADOR
- `requierePassword`: solo ADMIN requiere clave por defecto
- Usuarios iniciales: un ADMIN (password: `admin123`, hasheado BCrypt) y un OPERADOR
- Contraseñas almacenadas con BCrypt

### ConfiguracionGeneral
- Parámetros clave-valor. Claves iniciales:
  - `nombre_negocio`, `moneda_base`, `capital_inicial_cop`
  - `mostrar_tasa_dolar`, `zona_horaria`, `backup_dir`

### HistorialAuditoria
- Registra acción, entidad, id de entidad y detalle
- Usuario por defecto: `"sistema-local"`

---

## Catálogo inicial (DataInitializer)

### Categorías
Cuyes (1) · Gallinas (2) · Almuerzos (3) · Complementos (4) · Bebidas (5)

### Productos y composición de inventario
| Producto | afectaInventario | Composición |
|---|---|---|
| Cuy | ✅ | 1 cuy |
| 1/2 Cuy | ✅ | 0.5 cuy |
| 1/4 Cuy | ✅ | 0.25 cuy |
| Gallina | ✅ | 1 gallina |
| 1/2 Gallina | ✅ | 0.5 gallina |
| 1/8 Gallina | ✅ | 0.125 gallina |
| Almuerzo 1/8 gallina y 1/2 cuy | ✅ | 0.125 gallina + 0.5 cuy |
| Almuerzo 1/8 gallina y 1/4 cuy | ✅ | 0.125 gallina + 0.25 cuy |
| Consomé | ❌ | — |
| Consomé desmechado | ❌ | — |
| Porción Arroz | ❌ | — |
| Porción Papa | ❌ | — |
| Gaseosa | ❌ | — |
| Jugo Hit | ❌ | — |
| Agua | ❌ | — |
| Cerveza | ❌ | — |
| Pokeron | ❌ | — |

### Salones iniciales
- Salón principal (aforo 30)
- Terraza (aforo 20)
- Para llevar (sin aforo)

---

## Reglas de negocio implementadas (NO cambiar)

### Flujo de pedido
1. `crear()` — crea pedido con detalles; acepta abono inicial opcional
2. `agregarDetalle()` / `quitarDetalle()` / `actualizarDetalle()` — edita ítems mientras no esté finalizado/anulado
3. `registrarPago()` — registra pago; **NO toca el fondo todavía**; recalcula estado
4. `finalizarPedido()` — solo si estado = PAGADO:
   - descuenta inventario según `composicionSnapshot` de cada detalle
   - acredita los fondos por moneda (`COBRO_PEDIDO` en CajaMovimiento)
   - registra auditoría
5. `anularPedido()` — solo pedidos no FINALIZADOS

### Flujo de gasto
1. Descuenta el fondo indicado (valida saldo suficiente)
2. Si `insumoInventario` no es null: suma stock (ENTRADA en MovimientoInventario)
3. Registra `GASTO` en CajaMovimiento
4. Registra auditoría

### Moneda
- Monedas base: `COP`, `USD`, `NEQUI`
- Monedas NEQUI adicionales con prefijo `NEQUI_`
- COP y NEQUI: sin centavos. USD: máximo 2 decimales
- Conversión USD→COP usa la tasa vigente en `TasaCambio`
- El cambio (vueltas) al cliente nunca se registra en el fondo

### Inventario
- Al finalizar pedido: por cada detalle con `composicionSnapshot` no vacío, descuenta proporcional a cantidad
- Valida stock suficiente antes de finalizar (lanza `ValidacionException` si hay déficit)
- Movimientos manuales solo permiten tipo `entrada` o `ajuste` (no `salida` directa vía API)

### Cierre de caja
- Bloquea si hay pedidos activos (PENDIENTE, ABONADO_PARCIAL, PAGADO)
- Crea backup automático al cerrar
- Solo uno por fecha (unique constraint)

### Estados del pedido
- `PENDIENTE` → `ABONADO_PARCIAL` → `PAGADO` → `FINALIZADO` / `ANULADO`
- Un pedido solo pasa a `FINALIZADO` cuando está completamente pagado.
- `FINALIZADO` implica cierre operativo del pedido: descuenta inventario, acredita fondos.

---

## Frontend — Estado actual

### Estructura (`Frontend/src/`)

```
src/
├── App.jsx                         ← Router + QueryClientProvider
├── main.jsx                        ← ReactDOM.createRoot
├── index.css                       ← Tailwind v4 + design system
├── components/
│   ├── auth/LoginScreen.jsx
│   ├── layout/
│   │   ├── AppLayout.jsx           ← Guard de sesión → LoginScreen o layout
│   │   ├── Sidebar.jsx             ← Nav lateral colapsable (Zustand)
│   │   └── Topbar.jsx              ← Fecha, tasa USD, usuario
│   ├── shared/                     ← KpiCard, ConfirmDialog, MoneyInput, ToggleChip, Pagination
│   ├── pedidos/                    ← OrdenCard, ProductoRow, ModalCrearPedido,
│   │                                  ModalDetallePedido, ModalCobrar, ModalAgregarItems
│   ├── configuracion/              ← ModalProducto, ModalSalon, ModalHistorialPrecios,
│   │                                  ModalCrearOperador, ModalCambiarPassword
│   ├── fondos/                     ← ModalAjuste, ModalEntrada, ModalTransferencia
│   ├── gastos/                     ← ModalGasto
│   ├── inventario/                 ← ModalMovimiento, ModalCompraInsumo
│   └── resumen/                    ← CierreCajaPanel
├── pages/
│   ├── dashboard/Dashboard.jsx     ← ✅ Implementado
│   ├── pedidos/PedidosPage.jsx     ← ✅ Implementado (prioritario)
│   ├── gastos/GastosPage.jsx       ← ✅ Implementado
│   ├── fondos/FondosPage.jsx       ← ✅ Implementado
│   ├── inventario/InventarioPage.jsx ← ✅ Implementado
│   ├── resumen/ResumenPage.jsx     ← ✅ Implementado (incluye CierreCajaPanel)
│   ├── configuracion/
│   │   ├── ConfiguracionPage.jsx   ← ✅ Tabs: Productos, Salones, Fondos, Tasa USD, Usuarios
│   │   └── tabs/*.jsx              ← ✅ Los 5 tabs implementados
│   └── Placeholder.jsx
├── services/                       ← 13 servicios (uno por dominio)
├── store/useSessionStore.js        ← Zustand: { usuario, sidebarColapsado }
├── hooks/
│   ├── usePedidoMutations.js
│   └── useModalKeyboard.js
└── utils/
    ├── format.js                   ← formatCOP, formatUSD, formatFecha, etc.
    ├── constants.js                ← ESTADO_BADGE, CAT_BADGE y constantes de dominio
    └── pedidoHelpers.js            ← esActivo, sortByReserva, calcularInsumos
```

### Módulos pendientes de implementar (sin UI)
- **Auditoría** — endpoint `/api/v1/auditoria` existe, sin página
- **Backups** — endpoint `/api/v1/backups` existe, sin página
- **Exportaciones CSV** — endpoints `/api/v1/exportaciones/*` existen, sin página

### Prioridad de trabajo
**El módulo de Pedidos es el más crítico.** Cualquier mejora, corrección o feature relacionada con pedidos tiene prioridad sobre otros módulos.

### Auth y sesión
- `AppLayout` verifica `useSessionStore.usuario`. Si es `null`, muestra `LoginScreen`.
- `LoginScreen` lista usuarios activos vía API, luego `POST /api/v1/sesion/login`.
- La sesión vive **solo en memoria Zustand** (no se persiste). Al recargar la página se pide login de nuevo.
- Los endpoints **sí están protegidos** (`SecurityConfig`, `app.security.enabled=true`): token firmado (12 h) en `Authorization: Bearer ...`. Públicos: login, `GET /usuarios`, health, Swagger. Solo ADMIN: usuarios, configuración, backups. Cubierto por `SeguridadEndpointsTest`.
- ADMIN requiere password; OPERADOR no.

### Design system (CSS — `index.css`)
Clases utilitarias propias en `@layer components`:

| Grupo | Clases |
|---|---|
| Layout | `.layout`, `.main`, `.page-content`, `.page-header`, `.page-title` |
| Sidebar | `.sidebar`, `.sidebar.colapsado`, `.nav-item`, `.nav-item.activo` |
| Topbar | `.topbar`, `.topbar-chip` |
| Cards | `.card`, `.card-header`, `.card-title`, `.card-body` |
| KPIs | `.kpi-grid`, `.kpi-card`, `.kpi-label`, `.kpi-value` |
| Tabs | `.tabs`, `.tab`, `.tab.activo` |
| Botones | `.btn`, `.btn-primary`, `.btn-secondary`, `.btn-danger`, `.btn-sm`, `.btn-icon` |
| Formularios | `.form-group`, `.form-label`, `.form-control`, `.form-error`, `.form-row`, `.form-row-3` |
| Modales | `.modal-overlay`, `.modal`, `.modal-lg`, `.modal-header`, `.modal-title`, `.modal-body`, `.modal-footer` |
| Badges | `.badge`, `.badge-success`, `.badge-danger`, `.badge-warning`, `.badge-info`, `.badge-gray` |
| Tablas | `.table-wrap` (+ estilos globales en `@layer base`) |
| Estados | `.empty-state`, `.loading-center`, `.spinner`, `.alert`, `.alert-error` |
| Toolbar | `.toolbar`, `.search-wrap` |

Paleta de marca (variables CSS Tailwind v4):
```css
--color-verde-oscuro: #1a4731
--color-verde-medio:  #2d6a4f
--color-verde-claro:  #40916c
--color-verde-hover:  #52b788
--color-verde-bg:     #f0faf4
--color-page:         #f4f6f8
```

---

## Principios de diseño

1. Velocidad operativa sobre elegancia visual.
2. Todo cálculo financiero final: en backend.
3. Toda operación crítica: transaccional (`@Transactional`).
4. Toda modificación crítica: auditada (`auditoriaService.registrar(...)`).
5. Separación estricta: controller → service interface → service impl → repository → entity.
6. `BigDecimal` siempre para dinero e inventario. Nunca `double`.
7. Los fondos COP, USD y NEQUI son separados; no se mezclan.
8. El diseño debe poder migrar a PostgreSQL sin cambios de lógica.
9. Diseñar para migración futura a backend remoto y autenticación por usuario.
10. No acoplar decisiones de negocio a SQLite de forma irreversible.
11. Cada módulo debe tener vista de listado y vista de detalle.

---

## Convenciones de código

### Backend
- Interfaces en `fachadaServices/services/XxxService/IXxxService.java`
- Implementaciones: `XxxServiceImpl implements IXxxService`
- Controllers inyectan `IXxxService` (nunca la impl)
- Mappers: interfaces MapStruct con `@Mapper(componentModel = "spring")`
- `default` methods con lógica personalizada deben anotarse con `@Named(...)` si retornan tipos comunes (`String`, `BigDecimal`)
- Repositories extienden `JpaRepository<Entity, ID>` sin `@Repository`
- Entidades usan `@PrePersist` / `@PreUpdate` para timestamps en zona `America/Bogota`
- Optimistic locking con `@Version Long version` en entidades con concurrencia (Fondo, Gasto, Inventario, Pedido, Producto)
- Los controladores no contienen lógica de negocio compleja.
- Las excepciones de negocio deben ser explícitas y centralizadas.
- Guardar timestamps de creación y actualización.

### Frontend
- **Fetching**: TanStack Query (`useQuery` / `useMutation`). No usar `useState + useEffect` para APIs.
- **queryKey**: array descriptivo `["recurso", filtro]` p.ej. `["pedidos", false]`.
- **Mutaciones**: invalidar el queryKey afectado con `queryClient.invalidateQueries(...)`.
- **Modales**: componentes independientes con prop `onClose`. Se abren con `useState` local.
- **Formularios**: react-hook-form + zod. Validación de negocio en backend; frontend solo formato/requerido.
- **Números/dinero**: usar `formatCOP(valor)` de `utils/format.js`. Nunca `toLocaleString`.
- **Iconos**: lucide-react exclusivamente.
- **No crear archivos CSS nuevos** — todo va en clases Tailwind o en las clases del design system de `index.css`.
- **Electron**: no tocar carpeta `electron/` sin instrucción explícita.
- **Zustand**: solo almacena `{ usuario, sidebarColapsado }`. Datos de API con React Query.
- **Paginación**: usar `Pagination` de `shared/`. No reimplementar inline.
- Crear vistas de listado y detalle por módulo.
- Mantener diseño consistente de admin panel.

---

## UX/UI obligatoria

Admin panel de escritorio, no landing page.

- Sidebar visible permanentemente en escritorio; colapsable en pantallas pequeñas.
- Estructura estándar: KPIs arriba → filtros → listado/detalle abajo.
- Formularios rápidos con validación cerca del campo.
- Modales solo cuando aceleren la operación.
- Confirmaciones solo en acciones destructivas o irreversibles.
- Estados de pedido con color y texto visibles.
- Búsqueda rápida de productos al crear pedido.

### Pantallas implementadas / pendientes
| Pantalla | Ruta | Estado |
|---|---|---|
| Dashboard | `/` | ✅ Implementado |
| Pedidos | `/pedidos` | ✅ Implementado — **prioridad #1** |
| Resumen del día | `/resumen` | ✅ Implementado (incluye Cierre de Caja) |
| Inventario | `/inventario` | ✅ Implementado |
| Gastos | `/gastos` | ✅ Implementado |
| Capital y fondos | `/fondos` | ✅ Implementado |
| Configuración (Productos, Salones, Fondos, Tasa USD, Usuarios) | `/configuracion` | ✅ Implementado |
| Auditoría | — | ⏳ Pendiente — endpoint listo |
| Backups | — | ⏳ Pendiente — endpoint listo |
| Exportaciones CSV | — | ⏳ Pendiente — endpoints listos |

---

## Decisiones razonables ya tomadas

- La moneda base del sistema es COP.
- Nequi se maneja como fondo separado, aunque contablemente se consolide en COP.
- Un pedido puede quedar `PAGADO` antes de `FINALIZADO`.
- `FINALIZADO` implica cierre operativo del pedido.
- El descuento de inventario se hace al finalizar, no al crear el pedido.
- Los productos manuales no afectan inventario por defecto.
- La tasa USD activa se toma desde configuración al registrar un pago en USD.
- Todo movimiento de caja debe quedar trazado.
- Todo cambio crítico debe quedar listo para auditoría.

---

## Qué NO hacer

- No convertir esto en POS de mesas genérico sin contexto del asadero.
- No simplificar pagos mezclando Nequi con efectivo COP.
- No eliminar historial de movimientos de caja o inventario.
- No calcular inventario solo con stock agregado sin historial.
- No modelar pedidos sin detalle de pagos.
- No hacer toda la lógica en React.
- No proponer Firebase, MongoDB, Supabase o stacks alternos para el núcleo.
- No usar arquitectura innecesariamente compleja para un entorno local MVP.
- No omitir restricciones de integridad por comodidad.
- No asumir que un producto manual impacta inventario si no tiene composición definida.
- No calcular totales de pedido en el frontend — siempre vienen del backend en el DTO.
- No acreditar fondos en `registrarPago` — el fondo se acredita al **finalizar**.
- No eliminar `composicionSnapshot` de `PedidoDetalle` — es el registro histórico.
- No usar `@Autowired` — usar constructor injection vía Lombok.
- No inyectar `XxxServiceImpl` directamente — siempre la interfaz `IXxxService`.
- No añadir `@Repository` a interfaces Spring Data — es redundante.
- No hardcodear reglas de composición de inventario en frontend.

---

## Convenciones de respuesta de Codex

Cuando generes cambios en este repositorio:

1. No dar explicaciones genéricas.
2. Justificar decisiones importantes en términos de operación del asadero.
3. Si hay ambigüedad, resolver con una decisión razonable y explícita.
4. Si la decisión puede afectar dinero, inventario o auditoría, explicarla claramente.
5. Entregar resultados accionables que compilen.
6. No introducir librerías o frameworks no solicitados sin justificar su valor.
7. Si cambias estructura del proyecto, explicar impacto y motivo.
8. Si generas UI, respetar el layout sidebar + topbar + contenido operativo.
9. Al agregar un endpoint nuevo: agregar también su contrato en esta documentación.
10. Al agregar un servicio nuevo: crear siempre la interfaz `IXxxService` primero.

---

## Flujo de trabajo Git (GitFlow)

### Ramas permanentes
| Rama | Propósito |
|---|---|
| `main` | Producción estable. Solo recibe merges de `release/*` y `hotfix/*`. |
| `develop` | Integración continua. Base de todo trabajo nuevo. Rama por defecto. |

### Ramas temporales
| Tipo | Naming | Sale de | Vuelve a |
|---|---|---|---|
| Feature | `feature/nombre-corto` | `develop` | `develop` |
| Release | `release/X.Y.Z` | `develop` | `main` + `develop` |
| Hotfix | `hotfix/descripcion` | `main` | `main` + `develop` |

### Reglas
- **Nunca** commits directos a `main`.
- **Nunca** commits directos a `develop` (excepto fixes triviales de una línea).
- Todo trabajo nuevo va en `feature/*` primero.
- Releases del instalador Electron se publican **siempre desde `main`**.
- Versionado semver en `Frontend/package.json`.

---

## Definición de terminado

Una tarea se considera realmente terminada solo si:

- compila sin errores,
- los tests existentes siguen pasando (`mvn test`, `vitest run`),
- respeta el dominio del asadero,
- no rompe las reglas de negocio documentadas,
- no compromete trazabilidad (caja, inventario, auditoría),
- incluye validaciones en backend,
- conserva coherencia con el stack y la arquitectura,
- deja base escalable para iteraciones futuras.

---

## Cuándo actualizar este archivo

Este archivo debe actualizarse cuando una tarea:

- agregue o modifique un endpoint REST,
- implemente o modifique una pantalla del frontend,
- cambie la estructura de paquetes o archivos,
- agregue una dependencia nueva al stack,
- cambie una regla de negocio implementada,
- tome una decisión arquitectónica que no estaba documentada.

Proponer los cambios concretos al archivo antes de cerrar la conversación. No actualizar silenciosamente.

---

## Instrucción final

Pensar siempre en un sistema real de operación diaria: rápido para cobrar, claro para revisar pendientes, confiable para dinero e inventario, y simple de mantener.

El backend está implementado y funcionando. El frontend es el frente de trabajo actual.
