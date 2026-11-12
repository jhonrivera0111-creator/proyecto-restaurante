# Casa Verde — Explicación del código

Guía para entender **qué hace cada parte del código** y cómo se conectan. Los archivos principales ya tienen comentarios en español dentro del código (busca bloques `/** ... */` y `// ---`). Este documento es el mapa.

---

## 1. Cómo leer el proyecto

```
proyecto-restaurante/
├── Backend/    ← API en Java (Spring Boot). Toda la lógica de negocio.
├── Frontend/   ← Pantallas en React. Solo muestra datos y llama a la API.
├── docs/       ← Documentación
└── docker-compose.yml
```

**Camino de una acción** (ejemplo: "finalizar un pedido"):

```
Botón "Finalizar" (React)
  → hooks/usePedidoMutations.js (finalizarMut)
  → services/pedidoService.js   → PUT /api/v1/pedidos/{id}/finalizar
  → capaControladores/PedidoController.finalizar()
  → PedidoServiceImpl.finalizarPedido()          (reglas de negocio)
      → PedidoFinalizacionServiceImpl.ejecutar()
          → InventarioServiceImpl.descontarPorPedido()   (baja insumos)
          → CajaMovimientoServiceImpl.registrar()        (sube el fondo)
  → PedidoRepository.save() → SQLite
  ← JSON PedidoDTO → React refresca las pantallas afectadas
```

## 2. Backend — carpeta por carpeta

Raíz: `Backend/src/main/java/com/casaverde/backend/`

### 2.1 `capaControladores/` — la puerta HTTP
Una clase `@RestController` por recurso (Pedido, Producto, Fondo, Inventario, Gasto, Compra, Empleado...). Reciben la petición, validan el JSON con `@Valid` y llaman al servicio. **No tienen lógica de negocio.**

Anotaciones frecuentes: `@GetMapping/@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping` (verbo HTTP), `@PathVariable` (valor en la URL), `@RequestBody` (JSON del cuerpo), `@Operation` (texto de Swagger).

### 2.2 `fachadaServices/` — el negocio
| Subcarpeta | Contenido |
|---|---|
| `services/<modulo>/IXxxService` | **Interfaces**: el contrato de cada módulo |
| `services/<modulo>/XxxServiceImpl` | **Implementaciones**: las reglas, transacciones y auditoría |
| `dto/` | Objetos de entrada (`...Request`) y salida (`...DTO`) de la API |
| `mapper/` | MapStruct: convierte Entidad ↔ DTO automáticamente |

Módulos de servicios:

| Paquete | Servicios | Responsabilidad |
|---|---|---|
| `pedidoService` | PedidoServiceImpl, PedidoFinalizacionServiceImpl, CierreCajaServiceImpl, ResumenServiceImpl, ComprobanteServiceImpl, CuentaCobrarServiceImpl | Ciclo de vida del pedido, cierre de caja, resumen diario, comprobante, fiados |
| `cajaService` | FondoServiceImpl, CajaMovimientoServiceImpl, GastoServiceImpl | Saldos, libro de caja, gastos |
| `inventarioService` | InventarioServiceImpl | Stock y movimientos; descuento por pedido |
| `compraService` | CompraServiceImpl, ProveedorServiceImpl, AdelantoProveedorServiceImpl | Compras, abonos y adelantos a proveedores |
| `empleadoService` | EmpleadoServiceImpl | Nómina, adelantos, días trabajados, vacaciones |
| `configuracionService` | TasaCambioServiceImpl, ConfiguracionGeneralServiceImpl | Tasa USD y parámetros |
| `productoService` | ProductoServiceImpl, CategoriaProductoServiceImpl, SalonServiceImpl, UsuarioServiceImpl, MonetaryServiceImpl, AuditoriaServiceImpl, BackupServiceImpl, ExportacionServiceImpl | Catálogo, usuarios, redondeo/validación de monedas, auditoría, backups, CSV |

**Patrón de todas las implementaciones:**
- `@Service` → Spring la crea y la inyecta donde se pida su interfaz.
- `@RequiredArgsConstructor` (Lombok) → genera el constructor con los campos `final` (inyección de dependencias, sin `@Autowired`).
- `@Transactional` → todo el método es una transacción: si falla algo, se deshace todo.
- `auditoriaService.registrar(...)` → deja huella de la operación en `HistorialAuditoria`.

### 2.3 `capaAccesoADatos/` — la base de datos
- `models/entitys/` — **29 entidades JPA** (una por tabla). Ver sección 4.
- `models/enums/` — estados y tipos (`EstadoPedido`, `CajaMovimientoTipo`, `TipoMovimientoInventario`...).
- `repositories/` — interfaces `extends JpaRepository`. Spring genera el SQL (p. ej. `findByEstadoInOrderByCreadoEnDesc`). Sin `@Repository`.

### 2.4 `config/` — configuración transversal
| Archivo | Qué hace |
|---|---|
| `DataInitializer` | Al arrancar, siembra categorías, productos con receta, fondos, tasa 4200, inventario en 0 y el usuario ADMIN |
| `security/SecurityConfig` | Reglas de acceso: qué rutas son públicas, solo-ADMIN o autenticadas |
| `security/AuthTokenService` | Crea y valida el token firmado (HMAC-SHA256, 12 h) |
| `security/TokenAuthenticationFilter` | Lee el `Authorization: Bearer ...` de cada petición |
| `controllerAdvice/GlobalExceptionHandler` | Convierte excepciones en JSON uniforme con el código HTTP correcto |
| `controllerAdvice/ValidacionException` | Error de regla de negocio → HTTP 400 |
| `controllerAdvice/RecursoNoEncontradoException` | No existe el registro → HTTP 404 |
| `CorsConfig` | Qué orígenes pueden llamar a la API (Vite :5173, Electron) |
| `DataSourceConfig`, `dialect/SQLiteDialect` | Conexión y dialecto para que Hibernate hable con SQLite |
| `MonedaConstants` | Constantes COP, USD, NEQUI, EFECTIVO |
| `OpenApiConfig` | Documentación Swagger |

### 2.5 `resources/`
- `application.properties` — puerto 5020, ruta de la BD, seguridad activada, secreto del token.
- `db/migration/V1..V8` — migraciones **Flyway** (el esquema real de la BD, versionado). V4 teléfono y descuento, V5 efectivo en cierre, V6 pedido fiado, V7 orden de producto, V8 restricción de fiado.

## 3. Las reglas del dinero, paso a paso

### 3.1 Totales de un pedido — `PedidoTotales.of()`
```
subtotal       = Σ (precio unitario × cantidad)
total          = subtotal − descuento
abonado        = Σ pagos convertidos a COP
saldoPendiente = max(total − abonado, 0)
valorCambio    = max(abonado − total, 0)     ← vueltas
```
Se calculan **al vuelo**, nunca se guardan, para que no se desincronicen.

### 3.2 Moneda — `MonetaryServiceImpl`
Normaliza y valida montos: COP y NEQUI sin decimales, USD con 2; convierte USD→COP con la tasa vigente. La tasa usada se guarda en cada `Pago`.

### 3.3 Libro de caja — `CajaMovimientoServiceImpl.registrar()`
Único punto donde cambia un saldo. Valida que el fondo no quede negativo y escribe un `CajaMovimiento` con `saldoResultante`, `referenciaTipo` y `referenciaId` (origen). Tipos: COBRO_PEDIDO, GASTO, TRANSFERENCIA_*, COMPRA_PAGO, PAGO_EMPLEADO, ADELANTO_*, COBRO_CUENTA_COBRAR, AJUSTE_FONDO...

### 3.4 Finalizar pedido — `registrarCobroFinal()`
Agrupa los pagos por moneda y acredita cada fondo hasta cubrir el total. Lo que sobre (vueltas) no entra. Los cobros en COP van a la caja física **EFECTIVO**; USD y NEQUI a su propio fondo.

### 3.5 Inventario — `InventarioServiceImpl`
`calcularConsumos()` lee el JSON `composicionSnapshot` de cada línea (p. ej. `[{"insumo":"gallina","cantidad":0.5}]`), multiplica por la cantidad y suma por insumo. `validarStock()` impide finalizar si falta stock; `registrarSalida()` descuenta y deja un `MovimientoInventario`.

### 3.6 Cierre de caja — `CierreCajaServiceImpl.cerrar()`
Una vez por fecha; bloquea si hay pedidos activos; calcula esperado (saldo del sistema) vs contado (lo que cuenta el dueño, o por denominaciones); crea backup; guarda la diferencia.

## 4. Las entidades (tablas)

| Grupo | Entidades |
|---|---|
| Pedidos | `Pedido`, `PedidoDetalle` (con snapshots), `Pago` |
| Catálogo | `Producto`, `ProductoComposicionInventario` (receta), `ProductoPrecioHistorial`, `CategoriaProducto`, `Salon` |
| Caja | `Fondo` (PK = moneda), `CajaMovimiento`, `Gasto`, `TasaCambio`, `CierreCaja` |
| Inventario y compras | `Inventario`, `MovimientoInventario`, `Proveedor`, `Compra`, `AbonoCompra`, `AdelantoProveedor` |
| Personal | `Empleado`, `Adelanto`, `TrabajoDia`, `PagoEmpleado`, `Vacacion` |
| Cartera | `CuentaCobrar`, `AbonoCuentaCobrar` |
| Sistema | `Usuario`, `ConfiguracionGeneral`, `HistorialAuditoria` |

Anotaciones JPA: `@Entity`/`@Table` (tabla), `@Id`/`@GeneratedValue` (clave), `@Column` (columna), `@ManyToOne`/`@OneToMany` (relaciones), `@Version` (bloqueo optimista), `@PrePersist`/`@PreUpdate` (sellar fechas en hora de Bogotá). Lombok: `@Getter/@Setter/@Builder/@NoArgsConstructor`.

## 5. Frontend — carpeta por carpeta

Raíz: `Frontend/src/`

| Carpeta / archivo | Qué hace |
|---|---|
| `main.jsx` | Punto de entrada: monta React |
| `App.jsx` | Define las rutas (`/pedidos`, `/fondos`...) y el caché de React Query |
| `components/layout/AppLayout.jsx` | Marco: si no hay sesión muestra el login; si hay, sidebar + topbar + pantalla |
| `components/auth/LoginScreen.jsx` | Lista usuarios y hace `POST /sesion/login` |
| `pages/<modulo>/` | Una pantalla por módulo (Pedidos, Fondos, Inventario, Resumen, Empleados, Cuentas por cobrar, Configuración con 10 tabs) |
| `components/<modulo>/` | Modales y piezas de cada módulo (ModalCobrar, ModalCrearPedido, OrdenCard...) |
| `components/shared/` | Reutilizables: KpiCard, ConfirmDialog, MoneyInput, Pagination |
| `services/` | Un archivo por recurso; `api.js` es el cliente Axios base (agrega el token y traduce errores) |
| `hooks/usePedidoMutations.js` | Todas las acciones de escritura de pedidos; al terminar **invalidan** las consultas afectadas para refrescar pantallas |
| `store/useSessionStore.js` | Zustand: usuario logueado y estado del sidebar (solo memoria) |
| `utils/format.js` | `formatCOP`, `formatUSD`, fechas en formato colombiano |
| `utils/constants.js` | Colores/etiquetas de estados y constantes de dominio |
| `utils/pedidoHelpers.js` | Fracciones de producto, cálculo de insumos para mostrar, ordenar por reserva |
| `index.css` | Tailwind + clases del sistema de diseño (`.btn`, `.card`, `.modal`, `.badge`...) |
| `electron/` | Empaquetado como app de escritorio (opcional) |

**Patrón de datos en el frontend:**
- **Leer:** `useQuery({ queryKey: ["pedidos"], queryFn: ... })` → TanStack Query guarda en caché y refresca.
- **Escribir:** `useMutation(...)` → al éxito, `invalidateQueries` fuerza a recargar lo afectado (Pedidos, Fondos, Inventario, Resumen).
- **Formularios:** react-hook-form + zod (solo formato; la validación de negocio es del backend).
- **Sesión:** el login guarda `{id, nombre, rol, token}` en Zustand; `api.js` lo adjunta a cada petición y, si el servidor responde 401, cierra la sesión.

## 6. Seguridad

1. `POST /sesion/login` verifica la contraseña con **BCrypt** y devuelve un token.
2. El frontend envía `Authorization: Bearer <token>`.
3. `TokenAuthenticationFilter` valida firma y vencimiento y asigna el rol (`ROLE_ADMIN` / `ROLE_OPERADOR`).
4. `SecurityConfig`: login/health/Swagger públicos; usuarios, configuración y backups solo ADMIN; el resto autenticado.
5. ADMIN requiere contraseña; OPERADOR entra sin ella.

## 7. Pruebas

| Ubicación | Qué valida |
|---|---|
| `Backend/src/test/.../PedidoServiceTest` | Ciclo de vida del pedido, pagos, estados |
| `CasaVerdeBusinessRulesTests` | Reglas de punta a punta (caja, inventario, cierre) |
| `FondoServiceTest`, `InventarioServiceTest`, `CompraServiceTest`, `EmpleadoServiceTest` | Cada módulo |
| `arquitectura/ArquitecturaCapasTest` | ArchUnit: las capas no se saltan |
| `Frontend/src/**/__tests__` | Formato de dinero/fechas, fracciones, login, modales |

Comandos: `mvn test` (backend) y `npm run test:run` (frontend).

## 8. Glosario rápido

| Término | Significado |
|---|---|
| **DTO** | Objeto que viaja por la API, distinto de la entidad de BD |
| **Snapshot** | Copia de un dato al momento de la venta (precio, receta) para que el histórico no cambie |
| **Fondo** | "Caja" de una moneda (COP/EFECTIVO, USD, NEQUI) |
| **Fiado** | Pedido servido sin pago completo; la deuda queda en Cuentas por Cobrar |
| **Bloqueo optimista** | `@Version`: detecta ediciones simultáneas sin bloquear la BD |
| **Flyway** | Herramienta que aplica los scripts SQL de `db/migration` en orden |
| **ArchUnit** | Librería que prueba reglas de arquitectura como si fueran tests |
| **Invalidar consulta** | Decirle a React Query que un dato cambió y debe volver a pedirlo |

## 9. Dónde mirar primero (orden sugerido para estudiar)

1. `EstadoPedido.java` → entender los estados.
2. `Pedido.java` y `PedidoTotales.java` → qué es un pedido y cómo se calcula.
3. `PedidoServiceImpl.java` → crear, pagar, finalizar, fiar, anular.
4. `PedidoFinalizacionServiceImpl.java` + `CajaMovimientoServiceImpl.java` + `InventarioServiceImpl.java` → qué pasa con caja e inventario.
5. `PedidoController.java` → cómo se expone por HTTP.
6. `SecurityConfig.java` y `AuthTokenService.java` → seguridad.
7. Frontend: `App.jsx` → `api.js` → `usePedidoMutations.js` → `PedidosPage.jsx`.
