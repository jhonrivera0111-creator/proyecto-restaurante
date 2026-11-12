# CLAUDE.md

## Propósito

Este repositorio implementa un sistema de escritorio para la operación de un asadero/restaurante con foco en rapidez operativa, trazabilidad financiera, control de inventario y ejecución local.

Claude Code debe comportarse como:

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
| Routing | React Router v7 (`createBrowserRouter`; `createHashRouter` cuando `VITE_ELECTRON=true`) |
| Estado global | Zustand v5 (`useSessionStore`) |
| Fetching / caché | TanStack React Query v5 |
| HTTP | Axios — base URL `/api/v1` (relativa; nginx la proxea al backend) |
| Formularios | react-hook-form + zod |
| Iconos | lucide-react |
| Drag & drop | @dnd-kit/core + @dnd-kit/sortable + @dnd-kit/utilities (reordenar productos en Configuración) |
| Fechas | date-fns |
| Tests | Vitest + Testing Library (jsdom) |
| Desktop | Electron ^42.x + electron-builder 26.0.12 — carpeta `electron/` existe, no integrada con el build Docker aún |
| Puerto local | nginx `:80` → proxy `/api/*` → `backend:5020` |

### Despliegue (Docker Compose)
- `casaverde-backend` → `:5020` (Spring Boot JAR)
- `casaverde-frontend` → `:80` (nginx sirve React SPA + proxy `/api/*` al backend)
- El frontend usa URLs relativas `/api/v1/...`; nginx hace el proxy. No hay CORS en producción.
- En desarrollo (`npm run dev`): Vite corre en `:5173`, las llamadas a `/api/v1` van directo a `:5020` (CORS habilitado para ese origen).

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
│   └── electron/     ← Electron 42 (no integrado aún)
├── docker-compose.yml
└── CLAUDE.md
```

### Backend (`Backend/src/main/java/com/casaverde/backend/`)
├── CasaVerdeApplication.java                          ← @SpringBootApplication raíz
├── capaAccesoADatos/
│   ├── models/
│   │   ├── entitys/   ← 18 @Entity JPA
│   │   └── enums/     ← 4 enums de dominio
│   └── repositories/  ← 18 interfaces JpaRepository (sin @Repository)
├── capaControladores/ ← 15 @RestController
├── config/
│   ├── DataSourceConfig.java      ← DataSource SQLite
│   ├── CorsConfig.java            ← CORS (localhost:5173, :80, null/Electron)
│   ├── DataInitializer.java       ← CommandLineRunner — siembra datos iniciales + 4 métodos de backfill/migración idempotentes (backfillNequiAlias, backfillFinalizadoEn, backfillEstadosPedido, backfillPreciosDetalles)
│   ├── controllerAdvice/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ValidacionException.java
│   │   └── RecursoNoEncontradoException.java
│   └── dialect/
│       └── SQLiteDialect.java
└── fachadaServices/
    ├── dto/      ← 44 DTOs y Requests
    ├── mapper/   ← 15 @Mapper MapStruct
    └── services/
        ├── configuracionService/   ← IConfiguracionGeneralService, ITasaCambioService
        ├── pagoService/            ← IFondoService, ICajaMovimientoService
        ├── pedidoService/          ← IPedidoService, ICierreCajaService, IResumenService, IComprobanteService
        ├── productoService/        ← IProductoService, ICategoriaProductoService,
        │                              IInventarioService, ISalonService, IGastoService,
        │                              IUsuarioService, IMonetaryService,
        │                              IBackupService, IExportacionService, IAuditoriaService
        ├── compraService/          ← IProveedorService, ICompraService
        └── empleadoService/        ← IEmpleadoService
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
| PUT | `/reordenar` | Reordena el catálogo — body: array de ids en el nuevo orden (drag & drop en Configuración > Productos, se refleja también en el catálogo de Pedidos) |
| DELETE | `/{id}` | Soft-delete (marca `eliminado=true`; no aparece en listados) |

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
| POST | `/nequi/cuentas` | Crear una cuenta NEQUI adicional (ej. NEQUI_HAROLD) |
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
| PUT | `/{clave}` | Actualizar valor (`{valor}`) |
| DELETE | `/{clave}` | Eliminar parámetro |

### Salones — `/api/v1/salones`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista salones. `?soloActivos=true` |
| GET | `/{id}` | Detalle |
| POST | `/` | Crear |
| PUT | `/{id}` | Actualizar |
| PUT | `/{id}/toggle` | Activar / desactivar |
| DELETE | `/{id}` | Soft-delete (marca `eliminado=true`; no aparece en listados) |

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
| POST | `/` | Crear operador (`{nombre, password}`) — solo rol OPERADOR desde UI |
| PUT | `/{id}/toggle` | Activar / desactivar usuario |
| PUT | `/{id}/password` | Cambiar contraseña de un usuario |
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

### Proveedores — `/api/v1/proveedores`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista proveedores. `?soloActivos=true` filtra inactivos |
| POST | `/` | Crear proveedor (`{nombre, tipo, telefono?}`) |
| PUT | `/{id}` | Actualizar proveedor |
| PUT | `/{id}/toggle` | Activar / desactivar |
| GET | `/{id}/adelantos` | Lista adelantos prepagados del proveedor |
| POST | `/{id}/adelantos` | Registrar adelanto prepagado (`{monto, moneda, insumo, precioUnitarioAcordado, fecha}`) — debita fondo con ADELANTO_PROVEEDOR |

### Compras — `/api/v1/compras`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista compras. `?fecha=` y `?estado=` (PENDIENTE/ABONADO_PARCIAL/PAGADO) |
| GET | `/{id}` | Detalle de una compra (incluye abonos y totalAbonado/saldoPendiente) |
| POST | `/` | Registrar compra (`{proveedorId, insumo, cantidad, precioUnitario, fecha, observacion?}`) — suma inventario automáticamente |
| DELETE | `/{id}` | Anular compra (solo estado PENDIENTE, sin abonos) — revierte inventario |
| GET | `/{id}/abonos` | Lista abonos de una compra |
| POST | `/{id}/abonos` | Registrar abono (`{monto, moneda, fecha, adelantoProveedorId?}`) — si `adelantoProveedorId` presente, descuenta del adelanto prepagado en vez de debitar fondo |

### Cuentas por Cobrar — `/api/v1/cuentas-cobrar`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista cuentas. Filtros: `estado` (PENDIENTE/ABONADO_PARCIAL/CANCELADO), `fecha`, `q` (búsqueda por cliente) |
| GET | `/{id}` | Detalle de una cuenta con abonos |
| POST | `/` | Registrar deuda (`{clienteNombre, concepto?, monto, celular?, fecha, observacion?}`) |
| DELETE | `/{id}` | Anular cuenta (solo PENDIENTE sin abonos) |
| GET | `/{id}/abonos` | Lista cobros de una cuenta |
| POST | `/{id}/abonos` | Registrar cobro (`{monto, moneda, fecha, observacion?}`) — **acredita** el fondo con COBRO_CUENTA_COBRAR |

### Empleados — `/api/v1/empleados`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista empleados. `?soloActivos=true` (default) |
| POST | `/` | Crear empleado (`{nombre, tipo, salario}`) |
| PUT | `/{id}` | Actualizar nombre y/o salario |
| PUT | `/{id}/toggle` | Activar / desactivar |
| GET | `/{id}/adelantos` | Historial de adelantos |
| POST | `/{id}/adelantos` | Registrar adelanto — debita fondo inmediatamente con ADELANTO_EMPLEADO |
| GET | `/{id}/dias` | Días trabajados. `?soloNoPagados=true` |
| POST | `/{id}/dias` | Registrar un día trabajado (solo POR_DIA) |
| DELETE | `/{id}/dias/{diaId}` | Eliminar día no pagado |
| GET | `/{id}/pagos` | Historial de liquidaciones |
| GET | `/{id}/pagos/preview` | Vista previa del pago sin ejecutar (devuelve `EmpleadoResumenDTO`) |
| POST | `/{id}/pagos` | Liquidar — calcula, debita fondo con PAGO_EMPLEADO, marca adelantos y días como pagados |
| GET | `/{id}/vacaciones` | Historial de vacaciones |
| POST | `/{id}/vacaciones` | Registrar periodo de vacaciones (informativo) |
| DELETE | `/{id}/vacaciones/{vacId}` | Eliminar registro de vacación |

---

## Entidades implementadas

### Pedido
- Campos principales: `id`, `nombre`, `estado`, `mesa`, `salonId`, `fechaReserva`, `horaReserva`, `cantPersonas`, `paraLlevar`, `observacion`
- Totales calculados al vuelo por mapper: `totalCop`, `totalAbonado`, `saldoPendiente`, `valorCambio`
- Relaciones: `@OneToMany detalles` (cascade ALL), `@OneToMany pagos` (cascade ALL)
- `@Version` para bloqueo optimista
- Estado: `PENDIENTE` → `ABONADO_PARCIAL` → `PAGADO` → `FINALIZADO` / `ANULADO`

### PedidoDetalle
- `nombreSnapshot`: copia del nombre del producto al momento de agregar
- `composicionSnapshot`: JSON de la composición inventariable (snapshot, no FK a Producto)
- `precioUnitarioCop`, `cantidad`
- `@Version` para bloqueo optimista

### Pago
- `moneda` (COP | USD | NEQUI), `monto`, `montoCop`, `tasaUsada`
- Los pagos **no** afectan el fondo hasta que el pedido se **finaliza**
- `@Version` para bloqueo optimista

### Producto
- `afectaInventario` (boolean): si true, al finalizar el pedido se descuenta inventario
- `eliminado` (Boolean, default false): soft-delete — los listados filtran `eliminado=false`
- `orden` (Integer, default 0): posición manual en el catálogo (drag & drop en Configuración > Productos); los listados usan `OrderByOrdenAsc`, no alfabético
- `composicion`: lista de `ProductoComposicionInventario` (insumo + cantidad)
- `@Version` para bloqueo optimista

### ProductoPrecioHistorial
- Se crea automáticamente en `actualizarPrecio()`: guarda precio anterior, precio nuevo y motivo

### Fondo
- PK = `moneda` (string: "COP", "USD", "NEQUI")
- Soporta cuentas NEQUI adicionales con alias (ej. "NEQUI_HAROLD") — creadas dinámicamente vía API
- `alias`: nombre legible del fondo para mostrar en UI
- `@Version` para bloqueo optimista concurrente
- El fondo se acredita **al finalizar un pedido**, no al registrar el pago

### CajaMovimiento
- `tipo`: COBRO_PEDIDO | REVERSO_PAGO | GASTO | REVERSO_GASTO | AJUSTE_FONDO | ENTRADA_MANUAL | TRANSFERENCIA_SALIDA | TRANSFERENCIA_ENTRADA | COMPRA_PAGO | ADELANTO_EMPLEADO | PAGO_EMPLEADO | ADELANTO_PROVEEDOR | APLICACION_ADELANTO_PROVEEDOR | COBRO_CUENTA_COBRAR
- `referenciaTipo` + `referenciaId`: trazabilidad al origen del movimiento
- Cada movimiento guarda `saldoResultante` en el momento del registro

### Gasto
- `categoria`: insumos | servicios | equipos | otros
- `insumoInventario` + `cantidadInventario`: si se completan, el gasto suma stock al inventario
- Descuenta el fondo correspondiente en la misma transacción

### Inventario
- Un registro por insumo (`"gallina"`, `"cuy"`)
- `@Version` para bloqueo optimista
- Insumos iniciales: gallina (0), cuy (0)

### MovimientoInventario
- `tipo`: entrada | salida | ajuste
- `referenciaTipo`: pedido | gasto | ajuste_manual

### TasaCambio
- Historial completo; la vigente es la de mayor `vigenteDesde`
- Valor inicial: 1 USD = 4200 COP

### CierreCaja
- Único por fecha (`UNIQUE(fecha)`)
- Guarda esperado vs contado en los tres fondos y genera backup automático
- Bloqueado si hay pedidos en estado activo (PENDIENTE, ABONADO_PARCIAL, PAGADO)

### Usuario
- `rol`: ADMIN | OPERADOR
- `requierePassword`: solo ADMIN requiere clave por defecto
- Usuarios iniciales: solo un ADMIN (password: `admin123`, hasheado **BCrypt**) — sin OPERADOR seed; se crea manualmente desde Configuración cuando se necesite
- Contraseñas almacenadas con BCrypt (Spring Security Crypto) — no SHA-256

### Salon
- `nombre`, `aforo`, `activo`, `eliminado` (Boolean, default false): soft-delete — los listados filtran `eliminado=false`

### CategoriaProducto
- `nombre`, `orden` (posición en UI), `activo`

### Proveedor
- `nombre`, `tipo` (TipoProveedor: CUY/GALLINA/AMBOS/OTROS), `telefono` (nullable), `activo` (default true), `creadoEn`

### Compra
- `proveedor` (@ManyToOne), `proveedorNombre` (snapshot), `insumo`, `cantidad` (scale 4), `precioUnitario` (scale 0), `totalCop` (scale 0)
- `estado` (EstadoCompra: PENDIENTE → ABONADO_PARCIAL → PAGADO), `observacion`, `fecha` (LocalDate), `creadoEn`
- `@Version` para bloqueo optimista
- Al registrar: suma stock de `insumo` en Inventario con `referenciaTipo="compra"`
- Al anular (solo PENDIENTE sin abonos): revierte stock con `referenciaTipo="compra_anulada"`

### AbonoCompra
- `compra` (@ManyToOne), `monto`, `moneda`, `montoCop`, `tasaUsada` (nullable), `fondoDebitado`, `fecha` (LocalDate), `creadoEn`
- `@Version` para bloqueo optimista
- Debita el fondo vía `CajaMovimientoTipo.COMPRA_PAGO` con monto negativo

### ConfiguracionGeneral
- Parámetros clave-valor. Claves iniciales:
  - `nombre_negocio`, `moneda_base`, `capital_inicial_cop`
  - `mostrar_tasa_dolar`, `zona_horaria`, `backup_dir`
  - `limite_exento_nequi_mensual` (límite mensual exento de 4×1000 NEQUI), `cuenta_nequi_activa`

### HistorialAuditoria
- Registra acción, entidad, id de entidad y detalle
- Usuario por defecto: `"sistema-local"`

### Empleado
- `tipo`: QUINCENAL (salario fijo por quincena) | POR_DIA (tarifa diaria × días acumulados)
- `salario`: BigDecimal scale 0 — monto quincenal o tarifa diaria según tipo
- `@Version` para bloqueo optimista
- `activo` (default true)

### Adelanto
- `tipo`: EFECTIVO (debita el fondo inmediatamente) | ESPECIE (bien o servicio descontado del sueldo, no debita fondo — observación obligatoria)
- Se debita el fondo **inmediatamente** al registrar si tipo=EFECTIVO
- `pagado = false` hasta incluirse en una liquidación
- `montoCop` = equivalente en COP (USD se convierte con tasa vigente)
- `fondoDebitado` = moneda del fondo (tipo EFECTIVO) o "ESPECIE" (tipo ESPECIE)

### TrabajoDia
- Solo para empleados `POR_DIA`
- Los días no pagados se pueden eliminar (corrección de errores)
- Al liquidar, todos los días pendientes se marcan `pagado = true`

### PagoEmpleado
- Snapshot completo: `salarioBruto`, `totalAdelantos`, `totalNeto`, `montoPagado`, `montoCop`
- `tipo`: QUINCENA | COBRO_DIAS
- `fondoDebitado` = moneda elegida al pagar

### Vacacion
- Registro informativo de periodo sin trabajo
- Sin efecto en cálculos de pago — se puede eliminar sin restricciones

### AdelantoProveedor
- Dinero entregado al proveedor antes de recibir los insumos
- `monto`, `saldoRestante` (decrece por cada compra contra el adelanto), `insumo`, `precioUnitarioAcordado`
- `activo = false` cuando `saldoRestante = 0`
- Debita el fondo al registrar con `ADELANTO_PROVEEDOR`
- Al abonar una compra con `adelantoProveedorId`, descuenta de `saldoRestante` sin tocar el fondo

### AbonoCompra (modificado)
- Campo nuevo `adelantoProveedorId` (nullable): si presente, el abono se aplica contra el adelanto prepagado

### CuentaCobrar
- Deuda de un cliente (crédito otorgado en el momento del consumo)
- `clienteNombre`, `concepto`, `monto` (deuda original), `celular`, `fecha`
- Estado: PENDIENTE → ABONADO_PARCIAL → CANCELADO
- Al registrar: **no toca el fondo** (el dinero aún no ha entrado)
- Al registrar abono: **acredita el fondo** con `COBRO_CUENTA_COBRAR`

### AbonoCuentaCobrar
- `cuenta` (@ManyToOne), `monto`, `moneda`, `montoCop`, `tasaUsada`, `fondoAcreditado`, `fecha`

---

## Catálogo inicial (DataInitializer)

### Categorías (en orden de aparición en UI)
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
- Ninguno — sin seed. El negocio crea sus propios salones desde Configuración según su layout real.

---

## Reglas de negocio implementadas

### Flujo de pedido (NO cambiar este comportamiento)
1. `crear()` — crea pedido con detalles; acepta abono inicial opcional
2. `agregarDetalle()` / `quitarDetalle()` / `actualizarDetalle()` — edita ítems mientras no esté finalizado/anulado
3. `registrarPago()` — registra pago; **NO toca el fondo todavía**; recalcula estado
4. `finalizarPedido()` — solo si estado = PAGADO:
   - descuenta inventario según `composicionSnapshot` de cada detalle
   - acredita los fondos por moneda (`COBRO_PEDIDO` en CajaMovimiento, ref = pedidoId)
   - registra auditoría
5. `anularPedido()` — solo pedidos no FINALIZADOS; como el fondo nunca fue acreditado, no hay reversión

### Flujo de gasto
1. Descuenta el fondo indicado (valida saldo suficiente)
2. Si `insumoInventario` no es null: suma stock (ENTRADA en MovimientoInventario)
3. Registra `GASTO` en CajaMovimiento
4. Registra auditoría

### Moneda
- Monedas base: `COP`, `USD`, `NEQUI` (validadas por `MonetaryService`)
- Monedas NEQUI adicionales con prefijo `NEQUI_` (ej. `NEQUI_HAROLD`) — se crean vía API
- COP y NEQUI: sin centavos (escala 0). USD: máximo 2 decimales
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

### Backups automáticos
- Cron diario a las `00:00 America/Bogota` (`BackupServiceImpl.crearBackupProgramado`, requiere `@EnableScheduling` en `CasaVerdeApplication`)
- Catch-up al iniciar la app: si no existe backup de hoy (la app no corre 24/7), crea uno al arrancar — no crea uno en cada arranque incondicionalmente
- Independiente del backup que genera el cierre de caja

---

## Frontend — Estado actual y convenciones

### Estructura de archivos (`Frontend/src/`)

```
src/
├── App.jsx                         ← Router (createBrowserRouter / createHashRouter en Electron) + QueryClientProvider
├── main.jsx                        ← ReactDOM.createRoot
├── index.css                       ← Tailwind v4 + @layer components (design system)
├── components/
│   ├── auth/LoginScreen.jsx        ← Pantalla de login local
│   ├── layout/
│   │   ├── AppLayout.jsx           ← Shell: guard de sesión → LoginScreen o layout
│   │   ├── Sidebar.jsx             ← Nav lateral colapsable (Zustand)
│   │   └── Topbar.jsx              ← Barra superior (fecha, tasa USD, usuario)
│   ├── shared/                     ← KpiCard, ConfirmDialog, MoneyInput, ToggleChip, Pagination
│   ├── pedidos/                    ← OrdenCard, ProductoRow, ModalCrearPedido,
│   │                                  ModalDetallePedido, ModalCobrar, ModalAgregarItems
│   ├── configuracion/              ← ModalProducto, ModalSalon, ModalHistorialPrecios,
│   │                                  ModalActualizarPrecio, ModalCrearOperador, ModalCambiarPassword,
│   │                                  ModalCategoria
│   ├── fondos/                     ← ModalAjuste, ModalEntrada, ModalTransferencia
│   ├── gastos/                     ← ModalGasto
│   ├── inventario/                 ← ModalMovimiento, ModalRegistrarCompra, ModalDetalleCompra,
│   │                                  ModalProveedor, ModalAdelantoProveedor
│   └── resumen/                    ← CierreCajaPanel
├── pages/
│   ├── dashboard/Dashboard.jsx     ← ✅ Implementado
│   ├── pedidos/PedidosPage.jsx     ← ✅ Implementado (prioritario)
│   ├── fondos/FondosPage.jsx       ← ✅ Implementado (fondos + salidas de caja/gastos)
│   ├── inventario/
│   │   ├── InventarioPage.jsx      ← ✅ Tabs: Stock, Compras, Proveedores
│   │   └── tabs/*.jsx              ← ✅ ComprasTab, ProveedoresTab
│   ├── resumen/ResumenPage.jsx     ← ✅ Implementado (incluye CierreCajaPanel)
│   ├── configuracion/
│   │   ├── ConfiguracionPage.jsx   ← ✅ Tabs: Productos, Salones, Tasa USD, Fondos, Usuarios, Inventario, Categorías, Auditoría, Backups, Reportes
│   │   └── tabs/*.jsx              ← ✅ 10 tabs: ProductosTab, SalonesTab, TasaTab, FondosTab, UsuariosTab, InventarioTab (ajuste de stock), CategoriasTab, AuditoriaTab, BackupsTab, ReportesTab
│   └── Placeholder.jsx             ← Componente vacío para módulos pendientes
├── services/                       ← 15 archivos (14 servicios de dominio + api.js instancia Axios)
│   │  api.js (instancia Axios base), pedidoService, productoService (incluye categoriaService),
│   │  inventarioService, fondoService, gastoService, salonService, tasaService,
│   │  cierreCajaService, resumenService, configuracionService, usuarioService,
│   │  exportacionService, auditoriaService, backupService,
│   │  proveedorService, compraService, empleadoService
├── store/useSessionStore.js        ← Zustand: { usuario, sidebarColapsado }
│   │  (la tasa USD viene de React Query, no de Zustand)
├── hooks/
│   ├── usePedidoMutations.js       ← Mutations TanStack Query para pedidos
│   └── useModalKeyboard.js         ← Cierre de modales con tecla Escape
└── utils/
    ├── format.js                   ← formatCOP, formatUSD, formatDecimal, formatFecha,
    │                                  formatHora, formatCreadoEn, formatFechaLabel
    ├── constants.js                ← ESTADO_BADGE, CAT_BADGE y demás constantes de dominio
    └── pedidoHelpers.js            ← esActivo, sortByReserva, calcularInsumos, formatInsumo,
                                       calcularInsumosDetallado, normalizarFracciones, REGLAS_FRACCION,
                                       formatFraccion, formatFechaReserva, formatHoraReserva,
                                       formatReservaDisplay, esEnCursoPedido, toTitleCase, capFirst,
                                       ESTADOS_ACTIVOS
```

> **Nota de diseño:** Los gastos (salidas de caja) no tienen página propia — se registran desde `/fondos`. Es una decisión intencional, no deuda técnica.

> **Nota de diseño:** Compras y Proveedores viven en `/inventario`, no en Configuración. Antes había dos caminos separados para sumar stock: el botón "+Registrar entrada" (creaba un `Gasto` genérico, sin proveedor ni trazabilidad de deuda) y la tab Compras de Configuración (creaba una `Compra` formal a proveedor, con abonos). Se unificaron: `ModalCompraInsumo` se eliminó; "+Registrar compra" en Inventario ahora usa el mismo flujo real de Compra a proveedor (con abono inicial opcional y creación rápida de proveedor inline si no hay ninguno).

### Prioridad de trabajo actual
**El módulo de Pedidos es el más crítico.** Trabajar primero cualquier mejora, corrección o feature relacionada con pedidos antes de otros módulos.

### Auth y sesión
- `AppLayout` verifica `useSessionStore.usuario`. Si es `null`, muestra `LoginScreen`.
- `LoginScreen` llama a `GET /api/v1/usuarios` para listar usuarios activos, luego `POST /api/v1/sesion/login`.
- La sesión vive **solo en memoria Zustand** (no se persiste en localStorage). Al recargar la página se pide login de nuevo.
- Los endpoints **sí están protegidos** (`SecurityConfig`, `app.security.enabled=true`): el login devuelve un token firmado (12 h) que `api.js` envía como `Authorization: Bearer ...`. Públicos: login, `GET /usuarios`, `/actuator/health`, Swagger. Solo ADMIN: `/usuarios/**`, `/configuracion/**`, `/backups/**`. Cubierto por `SeguridadEndpointsTest`.
- El usuario ADMIN requiere password; el OPERADOR no.

### Design system (CSS — `index.css`)
Clases utilitarias propias definidas en `@layer components`:

| Grupo | Clases disponibles |
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

**Paleta de marca** (variables CSS Tailwind v4):
```css
--color-verde-oscuro: #1a4731  /* sidebar, títulos */
--color-verde-medio:  #2d6a4f
--color-verde-claro:  #40916c  /* botones primarios, activo */
--color-verde-hover:  #52b788
--color-verde-bg:     #f0faf4  /* fondos sutiles */
--color-page:         #f4f6f8  /* fondo global */
```

### Convenciones de código frontend

- **Fetching**: siempre con TanStack Query (`useQuery` / `useMutation`). No usar `useState + useEffect` para llamadas a la API.
- **queryKey**: array descriptivo `["recurso", filtro]` p.ej. `["pedidos", false]`, `["resumen", fecha]`.
- **Mutaciones**: invalidar el queryKey afectado con `queryClient.invalidateQueries(...)` después de cada mutation exitosa.
- **Modales**: componentes independientes con prop `onClose`. Se abren con `useState` local en la página padre.
- **Formularios**: react-hook-form + zod. No validar en el backend y repetir la misma lógica en frontend — la validación de negocio vive en backend; en frontend solo validaciones de formato/requerido.
- **Números/dinero**: usar `formatCOP(valor)` de `utils/format.js`. Nunca formatear manualmente con `toLocaleString`.
- **Iconos**: lucide-react exclusivamente.
- **No crear nuevos archivos CSS** — todo estilo va en clases Tailwind o en las clases del design system de `index.css`.
- **Electron**: no tocar la carpeta `electron/` sin instrucción explícita.
- **Zustand**: solo almacena `{ usuario, sidebarColapsado }`. La tasa USD y otros datos de la API se obtienen con React Query — no guardar datos de API en el store global.
- **Paginación**: usar `Pagination` de `shared/` para tablas largas. No reimplementar lógica de páginas inline.

---

## Principios de diseño (no negociables)

1. Velocidad operativa sobre elegancia visual.
2. Todo cálculo financiero final: en backend.
3. Toda operación crítica: transaccional (`@Transactional`).
4. Toda modificación crítica: auditada (`auditoriaService.registrar(...)`).
5. Separación estricta: controller → service interface → service impl → repository → entity.
6. `BigDecimal` siempre para dinero e inventario. Nunca `double`.
7. Los fondos COP, USD y NEQUI son separados; no se mezclan como si fueran uno.
8. El diseño debe poder migrar a PostgreSQL sin cambios de lógica.

---

## Convenciones de código backend

- Interfaces de servicio en `fachadaServices/services/XxxService/IXxxService.java`
- Implementaciones: `XxxServiceImpl implements IXxxService`
- Controllers inyectan `IXxxService` (nunca la impl)
- Los mappers son interfaces MapStruct con `@Mapper(componentModel = "spring")`
- `default` methods en mappers con lógica personalizada deben anotarse con `@Named(...)` si retornan tipos comunes (`String`, `BigDecimal`) para evitar que MapStruct los aplique automáticamente en otros mappings
- Los repositories extienden `JpaRepository<Entity, ID>` sin `@Repository`
- Las entidades usan `@PrePersist` / `@PreUpdate` para timestamps en zona `America/Bogota`
- Optimistic locking en entidades con concurrencia: `@Version Long version` (Fondo, Gasto, Inventario, Pedido, PedidoDetalle, Pago, Producto)

---

## Convenciones de respuesta de Claude Code

Cuando respondas o generes cambios en este repositorio:

1. No dar explicaciones genéricas.
2. Justificar decisiones en términos de operación del asadero.
3. Si hay ambigüedad, resolver con una decisión razonable y explícita.
4. Si la decisión puede afectar dinero, inventario o auditoría, explicarla claramente.
5. Entregar resultados accionables que compilen.
6. No introducir librerías no solicitadas sin justificar su valor.
7. Si cambias estructura del proyecto, explicar impacto y motivo.
8. Si generas UI, respetar el layout sidebar + topbar + contenido operativo.
9. Al agregar un endpoint nuevo: agregar también su contrato en esta documentación.
10. Al agregar un servicio nuevo: crear siempre la interfaz `IXxxService` primero.

---

## Qué NO hacer

- No calcular totales de pedido en el frontend — siempre vienen del backend en el DTO.
- No acreditar fondos en `registrarPago` — el fondo se acredita al **finalizar**.
- No eliminar `composicionSnapshot` de `PedidoDetalle` — es el registro histórico.
- No simplificar pagos mezclando NEQUI con COP.
- No eliminar historial de movimientos de caja o inventario.
- No proponer Firebase, MongoDB, Supabase ni stacks alternos para el núcleo.
- No usar `@Autowired` — usar constructor injection vía Lombok.
- No inyectar `XxxServiceImpl` directamente — siempre la interfaz `IXxxService`.
- No añadir `@Repository` a las interfaces Spring Data — es redundante.
- No hardcodear reglas de composición de inventario en frontend.

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
| Inventario (Stock, Compras, Proveedores) | `/inventario` | ✅ Implementado (3 tabs — Compras/Proveedores viven aquí, no en Configuración) |
| Capital, fondos y salidas | `/fondos` | ✅ Implementado (los gastos son "salidas de caja" registradas desde aquí) |
| Configuración (Productos, Categorías, Salones, Tasa USD, Fondos NEQUI, Usuarios, Ajuste de stock, Auditoría, Backups, Reportes) | `/configuracion` | ✅ Implementado (10 tabs) |
| Exportaciones CSV | ResumenPage | ✅ Implementado — botones "Resumen CSV" y "Caja CSV" en ResumenPage |
| Empleados | `/empleados` | ✅ Implementado |
| Cuentas por Cobrar | `/cuentas-cobrar` | ✅ Implementado |

---

## Definición de terminado

Una tarea se considera realmente terminada solo si:

- compila sin errores,
- los tests existentes siguen pasando (`mvn test`),
- respeta el dominio del asadero,
- no rompe las reglas de negocio documentadas arriba,
- no compromete trazabilidad (caja, inventario, auditoría),
- incluye validaciones en backend,
- conserva coherencia con el stack y la arquitectura.

---

## Mantenimiento de este archivo

Este archivo es la fuente de verdad del proyecto. Se desactualiza cada vez que se agrega un endpoint, una pantalla, una entidad o una decisión técnica relevante.

**Claude Code debe evaluar si CLAUDE.md necesita actualización al final de cualquier tarea que:**
- agregue o modifique un endpoint REST,
- implemente o modifique una pantalla del frontend,
- cambie la estructura de paquetes o archivos,
- agregue una dependencia nueva al stack,
- cambie una regla de negocio implementada,
- tome una decisión arquitectónica que no estaba documentada.

Si la tarea califica, proponer los cambios concretos al CLAUDE.md antes de cerrar la conversación. No actualizar silenciosamente sin avisar al usuario qué cambió y por qué.

---

## Flujo de trabajo Git (GitFlow)

### Ramas permanentes
| Rama | Propósito |
|---|---|
| `main` | Producción estable. Solo recibe merges de `release/*` y `hotfix/*`. Cada merge a main genera un release con tag `vX.Y.Z`. |
| `develop` | Integración continua. Base de todo trabajo nuevo. Rama por defecto del repositorio. |

### Ramas temporales
| Tipo | Naming | Sale de | Vuelve a |
|---|---|---|---|
| Feature | `feature/nombre-corto` | `develop` | `develop` |
| Release | `release/X.Y.Z` | `develop` | `main` + `develop` |
| Hotfix | `hotfix/descripcion` | `main` | `main` + `develop` |

### Comandos habituales

```bash
# Empezar una feature
git checkout develop && git checkout -b feature/nueva-pantalla

# Terminar una feature (merge a develop)
git checkout develop && git merge --no-ff feature/nueva-pantalla && git branch -d feature/nueva-pantalla

# Preparar un release
git checkout develop && git checkout -b release/1.2.0
# (ajustar version en package.json, últimos fixes)
git checkout main && git merge --no-ff release/1.2.0 && git tag v1.2.0
git checkout develop && git merge --no-ff release/1.2.0
git branch -d release/1.2.0

# Fix urgente en producción
git checkout main && git checkout -b hotfix/fix-critico
# (aplicar el fix)
git checkout main && git merge --no-ff hotfix/fix-critico && git tag v1.1.1
git checkout develop && git merge --no-ff hotfix/fix-critico
git branch -d hotfix/fix-critico

# Publicar release del instalador (desde main)
git checkout main
$env:GH_TOKEN = "ghp_token_write"
cd Frontend && npm run publish:electron
```

### Reglas
- **Nunca** hacer commits directos a `main`.
- **Nunca** hacer commits directos a `develop` (excepto fixes triviales de una línea).
- Todo trabajo nuevo va en `feature/*` primero.
- Los releases del instalador Electron se publican **siempre desde `main`**.
- El versionado sigue semver: `MAJOR.MINOR.PATCH` → cambios en `Frontend/package.json` antes de hacer el release.

---

## Instrucción final para Claude Code

Pensar siempre en un sistema real de operación diaria: rápido para cobrar, claro para revisar pendientes, confiable para dinero e inventario, y simple de mantener.

El backend está implementado y funcionando. El frontend es el frente de trabajo actual.
