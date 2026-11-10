# Spec: Tier 3 — Módulo de Proveedores y Compras

**Fecha:** 2026-06-03  
**Rama:** `feature/tier3-proveedores-compras` (desde `develop`)  
**Scope:** Módulo completo de proveedores, compras formales, cuentas por pagar y abonos. Backend desde cero + frontend desde cero.

---

## Contexto

El asadero actualmente controla las compras de cuyes y gallinas en Excel (hojas "Yuli Gallinas", "Cuyes Amparo"). El sistema no tiene ninguna representación de proveedores ni de compras. Esto implica:

- No hay trazabilidad de cuánto se le debe a cada proveedor.
- Los ingresos de inventario se registran manualmente como gastos o movimientos sueltos.
- No hay historial de compras ni de abonos parciales.

El Tier 3 cierra esta brecha con un módulo que:
1. Registra proveedores (cuyes, gallinas, mixtos, otros).
2. Registra compras formales que aumentan inventario automáticamente.
3. Permite abonar parcialmente con cualquier fondo (COP/USD/NEQUI).
4. Mantiene el estado de cuenta por pagar (PENDIENTE → ABONADO_PARCIAL → PAGADO).

---

## Entidades nuevas

### `Proveedor`

```java
@Entity @Table(name = "proveedor")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Proveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProveedor tipo;        // CUY | GALLINA | AMBOS | OTROS

    private String telefono;           // nullable

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

### `Compra`

```java
@Entity @Table(name = "compra")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "proveedor_nombre", nullable = false)
    private String proveedorNombre;    // snapshot al momento de la compra

    @Column(nullable = false)
    private String insumo;             // "gallina" | "cuy"

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 19, scale = 0, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "total_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal totalCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado;       // PENDIENTE | ABONADO_PARCIAL | PAGADO

    private String observacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

### `AbonoCompra`

```java
@Entity @Table(name = "abono_compra")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AbonoCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda;             // "COP" | "USD" | "NEQUI" | "NEQUI_*"

    @Column(name = "monto_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;      // null si moneda = COP o NEQUI

    @Column(name = "fondo_debitado", nullable = false)
    private String fondoDebitado;      // moneda del fondo que se debitó

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
```

### Enums nuevos

```java
public enum TipoProveedor { CUY, GALLINA, AMBOS, OTROS }
public enum EstadoCompra  { PENDIENTE, ABONADO_PARCIAL, PAGADO }
```

### Nuevo tipo en `TipoCajaMovimiento`

Agregar al enum existente:
```java
COMPRA_PAGO
```

---

## Backend — Capas

### Repositories (sin `@Repository`)

```java
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActivoTrue();
}

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByFecha(LocalDate fecha);
    List<Compra> findByEstado(EstadoCompra estado);
    List<Compra> findByFechaAndEstado(LocalDate fecha, EstadoCompra estado);
}

public interface AbonoCompraRepository extends JpaRepository<AbonoCompra, Long> {
    List<AbonoCompra> findByCompraId(Long compraId);
    @Query("SELECT COALESCE(SUM(a.montoCop), 0) FROM AbonoCompra a WHERE a.compra.id = :compraId")
    BigDecimal sumMontoCopByCompraId(@Param("compraId") Long compraId);
}
```

### DTOs

**`ProveedorDTO`**
```java
{ id, nombre, tipo (TipoProveedor), telefono, activo, creadoEn }
```

**`ProveedorRequest`**
```java
{ nombre (not null, not blank), tipo (not null), telefono (nullable) }
```

**`CompraDTO`**
```java
{ id, proveedorId, proveedorNombre, insumo, cantidad, precioUnitario,
  totalCop, totalAbonado (BigDecimal calculado), saldoPendiente (calculado),
  estado, observacion, fecha, creadoEn, abonos: List<AbonoCompraDTO> }
```

`totalAbonado` y `saldoPendiente` se calculan en el **servicio** (no en el mapper — los mappers no inyectan repositories) usando `abonoCompraRepository.sumMontoCopByCompraId(id)` antes de construir el DTO.

**`CompraRequest`**
```java
{ proveedorId (not null), insumo (not null, "gallina"|"cuy"),
  cantidad (not null, > 0), precioUnitario (not null, > 0),
  observacion (nullable), fecha (not null, LocalDate) }
```

**`AbonoCompraDTO`**
```java
{ id, compraId, monto, moneda, montoCop, tasaUsada, fondoDebitado, fecha, creadoEn }
```

**`AbonoCompraRequest`**
```java
{ monto (not null, > 0), moneda (not null), fecha (not null, LocalDate) }
```

### Interfaces de servicio

**`IProveedorService`**
```java
List<ProveedorDTO> listar(boolean soloActivos);
ProveedorDTO crear(ProveedorRequest req);
ProveedorDTO actualizar(Long id, ProveedorRequest req);
ProveedorDTO toggle(Long id);
```

**`ICompraService`**
```java
List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado);
CompraDTO obtener(Long id);
CompraDTO registrar(CompraRequest req);
void anular(Long id);
AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req);
List<AbonoCompraDTO> listarAbonos(Long compraId);
```

### Lógica de `CompraServiceImpl.registrar()`

```
1. Buscar Proveedor por proveedorId — RecursoNoEncontradoException si no existe
2. Validar proveedor.activo — ValidacionException si inactivo
3. Calcular totalCop = cantidad × precioUnitario
4. Guardar Compra { proveedorNombre = proveedor.nombre (snapshot), estado = PENDIENTE }
5. Crear MovimientoInventario { tipo=entrada, insumo, cantidad, referenciaTipo="compra", referenciaId=compra.id }
6. Registrar auditoría ("Compra", compra.id, "Registrada compra de {cantidad} {insumo}")
```

Todo en `@Transactional`.

### Lógica de `CompraServiceImpl.registrarAbono()`

```
1. Obtener Compra — RecursoNoEncontradoException si no existe
2. ValidacionException si compra.estado == PAGADO
3. totalAbonado = abonoRepo.sumMontoCopByCompraId(compraId)
4. saldoPendiente = compra.totalCop - totalAbonado
5. Resolver montoCop (si USD: montoCop = monto × tasaVigente; si COP/NEQUI: montoCop = monto)
6. ValidacionException si montoCop > saldoPendiente ("El abono supera el saldo pendiente")
7. Debitar Fondo (req.moneda) — ValidacionException si saldo insuficiente
8. Crear CajaMovimiento { tipo=COMPRA_PAGO, referenciaTipo="compra", referenciaId=compraId }
9. Guardar AbonoCompra
10. Recalcular estado compra:
    nuevaTotal = totalAbonado + montoCop
    si nuevaTotal >= compra.totalCop → estado = PAGADO
    si nuevaTotal > 0 → estado = ABONADO_PARCIAL
    guardar compra con nuevo estado
11. Registrar auditoría ("AbonoCompra", abono.id, "Abono de {monto} {moneda}")
```

Todo en `@Transactional`.

### Lógica de `CompraServiceImpl.anular()`

```
1. Obtener Compra
2. ValidacionException si estado != PENDIENTE ("Solo se pueden anular compras sin abonos")
3. Verificar que no haya abonos (abonoRepo.findByCompraId(id).isEmpty())
4. Crear MovimientoInventario { tipo=ajuste, cantidad negativa, referenciaTipo="compra_anulada" }
5. Eliminar la compra (o marcar eliminado si se prefiere soft-delete)
6. Registrar auditoría
```

### Controllers

**`ProveedorController`** → `/api/v1/proveedores`
```
GET    /              → listar(?soloActivos=false por defecto)
POST   /              → crear
PUT    /{id}          → actualizar
PUT    /{id}/toggle   → toggle
```

**`CompraController`** → `/api/v1/compras`
```
GET    /              → listar(?fecha=, ?estado=)
GET    /{id}          → obtener
POST   /              → registrar
DELETE /{id}          → anular
GET    /{id}/abonos   → listarAbonos
POST   /{id}/abonos   → registrarAbono
```

---

## Frontend

### Services nuevos

**`proveedorService.js`**
```js
import api from "./api";

export const proveedorService = {
  listar: (soloActivos = false) =>
    api.get("/proveedores", { params: { soloActivos } }).then(r => r.data),
  crear: (data) => api.post("/proveedores", data).then(r => r.data),
  actualizar: (id, data) => api.put(`/proveedores/${id}`, data).then(r => r.data),
  toggle: (id) => api.put(`/proveedores/${id}/toggle`).then(r => r.data),
};
```

**`compraService.js`**
```js
import api from "./api";

export const compraService = {
  listar: (params = {}) => api.get("/compras", { params }).then(r => r.data),
  obtener: (id) => api.get(`/compras/${id}`).then(r => r.data),
  registrar: (data) => api.post("/compras", data).then(r => r.data),
  anular: (id) => api.delete(`/compras/${id}`).then(r => r.data),
  listarAbonos: (id) => api.get(`/compras/${id}/abonos`).then(r => r.data),
  registrarAbono: (id, data) => api.post(`/compras/${id}/abonos`, data).then(r => r.data),
};
```

### Archivos a crear (frontend)

| # | Ruta | Descripción |
|---|---|---|
| 1 | `Frontend/src/services/proveedorService.js` | Service CRUD proveedores |
| 2 | `Frontend/src/services/compraService.js` | Service compras + abonos |
| 3 | `Frontend/src/components/configuracion/ModalProveedor.jsx` | Modal crear/editar proveedor |
| 4 | `Frontend/src/components/configuracion/ModalRegistrarCompra.jsx` | Modal registrar compra |
| 5 | `Frontend/src/components/configuracion/ModalDetalleCompra.jsx` | Modal detalle + abonos |
| 6 | `Frontend/src/pages/configuracion/tabs/ProveedoresTab.jsx` | Tab gestión de proveedores |
| 7 | `Frontend/src/pages/configuracion/tabs/ComprasTab.jsx` | Tab compras con filtros |

### `ProveedoresTab.jsx`

```
queryKey: ["proveedores"]
Tabla: Nombre | Tipo (badge) | Teléfono | Estado | Acciones
Tipos: CUY → badge-success | GALLINA → badge-warning | AMBOS → badge-info | OTROS → badge-gray
Acciones: Pencil → ModalProveedor(modo editar) | Power → toggle(sin confirm)
Toolbar: botón "Nuevo proveedor" → ModalProveedor(modo crear)
```

### `ModalProveedor.jsx`

```
Props: proveedor (null = crear, objeto = editar), onClose
Campos (zod):
  - nombre: string, requerido, max 100
  - tipo: enum select, requerido (opciones: CUY, GALLINA, AMBOS, OTROS)
  - telefono: string, opcional, max 20
Título: "Nuevo proveedor" / "Editar proveedor"
Submit: crear o actualizar → onClose → invalidate ["proveedores"]
```

### `ComprasTab.jsx`

```
Estado local: fecha (string YYYY-MM-DD, default hoy), estado (string, default "")
queryKey: ["compras", { fecha, estado }]
Filtros en toolbar: date input + select estado (Todos | PENDIENTE | ABONADO_PARCIAL | PAGADO)
Tabla: Fecha | Proveedor | Insumo | Cantidad | Total COP | Abonado | Saldo | Estado | Acciones
Estado badge: PENDIENTE → badge-gray | ABONADO_PARCIAL → badge-warning | PAGADO → badge-success
Acciones:
  Eye → ModalDetalleCompra
  Trash2 → ConfirmDialog → anular (solo visible si estado=PENDIENTE)
Toolbar: botón "Registrar compra" → ModalRegistrarCompra
```

### `ModalRegistrarCompra.jsx`

```
Props: onClose
useQuery: proveedores activos para el select
Campos (zod):
  - proveedorId: number, requerido
  - insumo: select (gallina | cuy), requerido
  - cantidad: number > 0, step 0.25, requerido
  - precioUnitario: number > 0 (COP), requerido
  - fecha: date, default hoy, requerido
  - observacion: string, opcional
Total en tiempo real (useMemo): cantidad × precioUnitario, formateado con formatCOP
Submit: compraService.registrar → invalidate ["compras"] → onClose
```

### `ModalDetalleCompra.jsx`

```
Props: compraId, onClose
useQuery: compraService.obtener(compraId), queryKey: ["compra", compraId]

Sección superior (read-only):
  Proveedor | Insumo | Cantidad | Precio unitario | Total COP | Estado

Tabla de abonos:
  Fecha | Monto | Moneda | Total COP | Fondo debitado
  Fila totales: — | — | — | suma montoCop | —

Sección inferior (si estado != PAGADO) — formulario abono:
  Campos (zod):
    - monto: number > 0
    - moneda: select (COP | USD | NEQUI | cuentas NEQUI activas)
    - fecha: date, default hoy
  Mostrar saldo pendiente encima del formulario
  Submit: compraService.registrarAbono → invalidate ["compra", compraId] + ["compras"]
```

### `ConfiguracionPage.jsx` — cambios

Agregar dos tabs:
```js
{ id: "proveedores", label: "Proveedores" },
{ id: "compras",     label: "Compras"     },
```

---

## Archivos a crear (backend — 23)

| # | Ruta | Descripción |
|---|---|---|
| 1 | `.../entitys/Proveedor.java` | Entidad JPA |
| 2 | `.../entitys/Compra.java` | Entidad JPA |
| 3 | `.../entitys/AbonoCompra.java` | Entidad JPA |
| 4 | `.../enums/TipoProveedor.java` | Enum |
| 5 | `.../enums/EstadoCompra.java` | Enum |
| 6 | `.../repositories/ProveedorRepository.java` | Repository |
| 7 | `.../repositories/CompraRepository.java` | Repository |
| 8 | `.../repositories/AbonoCompraRepository.java` | Repository |
| 9 | `.../dto/ProveedorDTO.java` | DTO |
| 10 | `.../dto/ProveedorRequest.java` | Request DTO |
| 11 | `.../dto/CompraDTO.java` | DTO con totalAbonado calculado |
| 12 | `.../dto/CompraRequest.java` | Request DTO |
| 13 | `.../dto/AbonoCompraDTO.java` | DTO |
| 14 | `.../dto/AbonoCompraRequest.java` | Request DTO |
| 15 | `.../mapper/ProveedorMapper.java` | MapStruct mapper |
| 16 | `.../mapper/CompraMapper.java` | MapStruct mapper |
| 17 | `.../mapper/AbonoCompraMapper.java` | MapStruct mapper |
| 18 | `.../services/compraService/IProveedorService.java` | Interfaz |
| 19 | `.../services/compraService/ProveedorServiceImpl.java` | Implementación |
| 20 | `.../services/compraService/ICompraService.java` | Interfaz |
| 21 | `.../services/compraService/CompraServiceImpl.java` | Implementación |
| 22 | `.../capaControladores/ProveedorController.java` | Controller REST |
| 23 | `.../capaControladores/CompraController.java` | Controller REST |

## Archivos a modificar (backend — 2)

| Archivo | Cambio |
|---|---|
| `TipoCajaMovimiento.java` (enum) | Agregar `COMPRA_PAGO` |
| `FondoServiceImpl.java` | Verificar que `debitar()` es accesible; exponer si está package-private |

## Archivos a modificar (frontend — 1)

| Archivo | Cambio |
|---|---|
| `ConfiguracionPage.jsx` | +2 tabs (Proveedores, Compras) + imports + renders condicionales |

---

## Verificación end-to-end

1. **Proveedores:**
   - Ir a `/configuracion` → tab Proveedores → crear proveedor "Yuli Gallinas" tipo GALLINA
   - Editar: cambiar teléfono → cambio persiste
   - Toggle inactivo → badge cambia → no aparece en selector de compras

2. **Registrar compra:**
   - Tab Compras → "Registrar compra" → proveedor Yuli, gallina, cantidad 20, precio 25000, hoy
   - Verificar que estado = PENDIENTE
   - Ir a `/inventario` → verificar que stock de gallina aumentó en 20

3. **Abonar:**
   - Abrir detalle de la compra → saldo pendiente = $500.000
   - Registrar abono de $200.000 COP → estado = ABONADO_PARCIAL, saldo = $300.000
   - Ir a `/fondos` → verificar que aparece movimiento COMPRA_PAGO en el historial
   - Registrar segundo abono de $300.000 → estado = PAGADO
   - Verificar que el formulario de abono desaparece

4. **Anular compra:**
   - Crear compra nueva sin abonar → anular → verificar que inventario revierte
   - Intentar anular compra con abonos → debe mostrar error

5. **Tests:**
   - `./mvnw test` — tests existentes siguen en verde
