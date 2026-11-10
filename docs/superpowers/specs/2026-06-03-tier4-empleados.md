# Tier 4 — Módulo de Empleados: Spec de diseño

**Fecha:** 2026-06-03  
**Autor:** Harold Velasquez  
**Estado:** Aprobado

---

## Contexto

El asadero Casa Verde maneja empleados con dos modalidades de pago distintas: algunos cobran cada quincena (salario fijo) y otros son llamados por día (ej. domingos) y van acumulando días trabajados hasta pedir su cobro. Además, cualquier empleado puede recibir adelantos de salario que se descuentan en el siguiente pago. El sistema también registra periodos de vacaciones como referencia informativa.

Actualmente esto se maneja de forma manual (Excel). Este módulo lo formaliza dentro del sistema, conectado al libro de caja existente.

---

## Alcance

Este spec cubre únicamente el **módulo de Empleados**. El módulo de Turnos (calendario mensual de roles) se diseñará por separado en Tier 4b y dependerá de este módulo.

---

## Reglas de negocio

### Tipos de empleado

| Tipo | Lógica de pago |
|---|---|
| `QUINCENAL` | Salario fijo por quincena. Al liquidar: `bruto = salario`. |
| `POR_DIA` | Tarifa diaria fija. Al liquidar: `bruto = días_pendientes × salario`. |

### Adelantos
- Se registran en cualquier momento.
- Debitan el fondo elegido **inmediatamente** vía `CajaMovimientoTipo.ADELANTO_EMPLEADO`.
- Se marcan `pagado = false` hasta incluirse en una liquidación.
- Al liquidar, **todos** los adelantos pendientes se descuentan del bruto.

### Días trabajados (`TrabajoDia`)
- Solo aplica para empleados tipo `POR_DIA`.
- El admin registra cada día individualmente con su fecha.
- Los días no pagados se pueden eliminar (corrección de error).
- Al liquidar, **todos** los días pendientes se incluyen en el cálculo.

### Liquidación (`PagoEmpleado`)
1. `bruto`:
   - QUINCENAL → `salario` del empleado
   - POR_DIA → `count(días_pendientes) × salario`
2. `totalAdelantos` → suma de `montoCop` de todos los adelantos pendientes
3. `neto = bruto - totalAdelantos`
4. Validar `neto >= 0`. Si hay más adelantos que salario: error.
5. Convertir `neto` a la moneda elegida si no es COP (usando tasa vigente).
6. Debitar fondo con `CajaMovimientoTipo.PAGO_EMPLEADO` (monto negativo).
7. Marcar todos los adelantos pendientes como `pagado = true`.
8. Para POR_DIA: marcar todos los días pendientes como `pagado = true`.
9. Guardar `PagoEmpleado` con snapshot del resultado.
10. Registrar auditoría.

### Vacaciones
- Registro informativo de periodo sin trabajo.
- Sin efecto en cálculos de pago.
- Se pueden eliminar si se registraron por error.

---

## Entidades JPA

### `Empleado`
```
id             Long (PK, auto)
version        Long (@Version)
nombre         String (not null)
tipo           TipoEmpleado (not null) — QUINCENAL | POR_DIA
salario        BigDecimal (precision 19, scale 0, not null) — monto quincenal o tarifa diaria
activo         Boolean (@Builder.Default = true)
creadoEn       LocalDateTime (@PrePersist, zona America/Bogota)
```

### `Adelanto`
```
id             Long (PK, auto)
empleado       @ManyToOne(LAZY) → Empleado (not null)
monto          BigDecimal (precision 19, scale 2, not null) — monto en la moneda elegida
moneda         String (not null)
montoCop       BigDecimal (precision 19, scale 0, not null)
tasaUsada      BigDecimal (precision 19, scale 2, nullable) — solo si moneda=USD
fondoDebitado  String (not null)
fecha          LocalDate (not null)
observacion    String (nullable)
pagado         Boolean (@Builder.Default = false)
creadoEn       LocalDateTime (@PrePersist)
```

### `TrabajoDia`
```
id             Long (PK, auto)
empleado       @ManyToOne(LAZY) → Empleado (not null)
fecha          LocalDate (not null)
observacion    String (nullable)
pagado         Boolean (@Builder.Default = false)
creadoEn       LocalDateTime (@PrePersist)
```

### `PagoEmpleado`
```
id             Long (PK, auto)
empleado       @ManyToOne(LAZY) → Empleado (not null)
tipo           TipoPagoEmpleado (not null) — QUINCENA | COBRO_DIAS
periodoInicio  LocalDate (not null)
periodoFin     LocalDate (not null)
diasTrabajados Integer (nullable) — solo para COBRO_DIAS
salarioBruto   BigDecimal (precision 19, scale 0, not null)
totalAdelantos BigDecimal (precision 19, scale 0, not null)
totalNeto      BigDecimal (precision 19, scale 0, not null)
moneda         String (not null)
montoPagado    BigDecimal (precision 19, scale 2, not null) — monto en moneda elegida
montoCop       BigDecimal (precision 19, scale 0, not null)
tasaUsada      BigDecimal (precision 19, scale 2, nullable)
fondoDebitado  String (not null)
observacion    String (nullable)
creadoEn       LocalDateTime (@PrePersist)
```

### `Vacacion`
```
id             Long (PK, auto)
empleado       @ManyToOne(LAZY) → Empleado (not null)
fechaInicio    LocalDate (not null)
fechaFin       LocalDate (not null)
observacion    String (nullable)
creadoEn       LocalDateTime (@PrePersist)
```

---

## Enums nuevos

```java
// TipoEmpleado.java
public enum TipoEmpleado { QUINCENAL, POR_DIA }

// TipoPagoEmpleado.java
public enum TipoPagoEmpleado { QUINCENA, COBRO_DIAS }
```

`CajaMovimientoTipo.java` — agregar al final:
```java
ADELANTO_EMPLEADO,
PAGO_EMPLEADO
```

`FondoMapper.java` — el switch exhaustivo necesita dos casos nuevos:
```java
case ADELANTO_EMPLEADO -> "salida";
case PAGO_EMPLEADO     -> "salida";
```

---

## API REST — `/api/v1/empleados`

### Empleados

| Método | Ruta | Body / Params | Descripción |
|---|---|---|---|
| GET | `/` | `?soloActivos=true` | Lista empleados |
| POST | `/` | `EmpleadoRequest` | Crear empleado |
| PUT | `/{id}` | `EmpleadoRequest` | Actualizar nombre y/o salario |
| PUT | `/{id}/toggle` | — | Activar / desactivar |

### Adelantos

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| GET | `/{id}/adelantos` | — | Historial de adelantos |
| POST | `/{id}/adelantos` | `AdelantoRequest` | Registrar adelanto (debita fondo) |

### Días trabajados (solo POR_DIA)

| Método | Ruta | Body / Params | Descripción |
|---|---|---|---|
| GET | `/{id}/dias` | `?soloNoPagados=true` | Lista días trabajados |
| POST | `/{id}/dias` | `TrabajoDiaRequest` | Registrar un día |
| DELETE | `/{id}/dias/{diaId}` | — | Eliminar día no pagado |

### Pagos

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| GET | `/{id}/pagos` | — | Historial de liquidaciones |
| GET | `/{id}/pagos/preview` | — | Vista previa del pago — devuelve `EmpleadoResumenDTO` sin ejecutar nada |
| POST | `/{id}/pagos` | `PagoEmpleadoRequest` | Liquidar (calcula y debita) |

### Vacaciones

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| GET | `/{id}/vacaciones` | — | Historial de vacaciones |
| POST | `/{id}/vacaciones` | `VacacionRequest` | Registrar periodo |
| DELETE | `/{id}/vacaciones/{vacId}` | — | Eliminar registro |

---

## DTOs y Requests

```
EmpleadoDTO        { id, nombre, tipo, salario, activo, creadoEn }
EmpleadoRequest    { nombre, tipo, salario }

AdelantoDTO        { id, empleadoId, monto, moneda, montoCop, tasaUsada, fondoDebitado, fecha, observacion, pagado, creadoEn }
AdelantoRequest    { monto, moneda, fecha, observacion? }

TrabajoDiaDTO      { id, empleadoId, fecha, observacion, pagado, creadoEn }
TrabajoDiaRequest  { fecha, observacion? }

PagoEmpleadoDTO    { id, empleadoId, tipo, periodoInicio, periodoFin, diasTrabajados, salarioBruto,
                     totalAdelantos, totalNeto, moneda, montoPagado, montoCop, tasaUsada,
                     fondoDebitado, observacion, creadoEn }
PagoEmpleadoRequest { moneda, periodoInicio, periodoFin, observacion? }

VacacionDTO        { id, empleadoId, fechaInicio, fechaFin, observacion, creadoEn }
VacacionRequest    { fechaInicio, fechaFin, observacion? }

EmpleadoResumenDTO { empleado: EmpleadoDTO, adelantosPendientes: List<AdelantoDTO>,
                     diasPendientes: List<TrabajoDiaDTO>, totalAdelantosCop: BigDecimal,
                     totalDiasPendientes: Integer, salarioBrutoEstimado: BigDecimal,
                     netoEstimado: BigDecimal }
```

`EmpleadoResumenDTO` es el DTO que devuelve `GET /{id}/pagos/preview` — permite mostrar el resumen en `ModalLiquidar` antes de confirmar.

---

## Arquitectura backend

```
fachadaServices/services/empleadoService/
  IEmpleadoService.java
  EmpleadoServiceImpl.java

fachadaServices/mapper/
  EmpleadoMapper.java        (Empleado ↔ EmpleadoDTO, Adelanto ↔ AdelantoDTO,
                              TrabajoDia ↔ TrabajoDiaDTO, PagoEmpleado ↔ PagoEmpleadoDTO,
                              Vacacion ↔ VacacionDTO)

capaControladores/
  EmpleadoController.java

capaAccesoADatos/repositories/
  EmpleadoRepository.java
  AdelantoRepository.java
  TrabajoDiaRepository.java
  PagoEmpleadoRepository.java
  VacacionRepository.java
```

Un único mapper `EmpleadoMapper` agrupa todos los mappings del módulo (patrón ya usado con `InventarioMapper`).

Un único servicio `IEmpleadoService` / `EmpleadoServiceImpl` maneja toda la lógica (empleados, adelantos, días, pagos, vacaciones). Si creciera demasiado, se puede extraer `IAdelantoService`, pero por YAGNI un solo service es suficiente.

---

## Frontend

### Archivos nuevos
```
Frontend/src/services/empleadoService.js
Frontend/src/components/empleados/ModalEmpleado.jsx
Frontend/src/components/empleados/ModalDetalleEmpleado.jsx
Frontend/src/components/empleados/ModalLiquidar.jsx
Frontend/src/pages/empleados/EmpleadosPage.jsx
```

### Archivos a modificar
```
Frontend/src/App.jsx              — agregar ruta /empleados
Frontend/src/components/layout/Sidebar.jsx  — agregar ítem Empleados
```

### `EmpleadosPage`
- Toolbar: "Nuevo empleado" + toggle Activos/Todos
- Tabla: Nombre | Tipo | Salario | Estado | Acciones (Editar, Toggle, Ver detalle)
- `ModalEmpleado`: campos nombre, tipo (select QUINCENAL/POR_DIA), salario (número entero)

### `ModalDetalleEmpleado` — modal-lg con 4 sub-tabs

**Tab Adelantos:**
- Tabla: Fecha | Monto | Moneda | Fondo | Estado (Pendiente/Pagado)
- Formulario inline: monto (number), moneda (select fondos), fecha (date)
- Botón "Registrar adelanto"

**Tab Días trabajados** (solo visible si tipo = POR_DIA):
- Contador visible: `X días pendientes — $YYY estimado`
- Tabla: Fecha | Observación | Estado | Acción (eliminar si no pagado)
- Botón "Registrar día"

**Tab Pagos:**
- Tabla historial: Fecha | Periodo | Bruto | Adelantos | Neto | Moneda | Fondo
- Botón "Registrar pago" → abre `ModalLiquidar`

**Tab Vacaciones:**
- Tabla: Inicio | Fin | Días | Observación | Acción (eliminar)
- Formulario inline: fechaInicio, fechaFin, observacion

### `ModalLiquidar`
- Al abrir: llama `GET /{id}/pagos/preview` para obtener `EmpleadoResumenDTO`
- Muestra resumen calculado: días pendientes, adelantos pendientes, bruto, neto
- Admin completa: moneda (select fondos), periodoInicio, periodoFin, observacion
- Botón "Confirmar pago" → POST `/{id}/pagos`

---

## Conexión con módulos existentes

- `ICajaMovimientoService.registrar(...)` — se usa para debitar fondo en adelantos y pagos (monto negativo)
- `ITasaCambioService.getValorVigente()` — conversión USD→COP
- `IAuditoriaService.registrar(...)` — auditoría en todas las operaciones de escritura
- `fondoService.listar()` en frontend — para el select de moneda en adelantos y pago

---

## Validaciones backend

- `nombre` obligatorio y no vacío
- `salario > 0`
- Al registrar adelanto: validar fondo tiene saldo suficiente (el propio `cajaMovimientoService` lo hace)
- Al registrar día: validar que el empleado es tipo `POR_DIA`
- Al eliminar día: validar `pagado = false`
- Al liquidar: validar `neto >= 0`; si negativo → "Los adelantos ($X) superan el salario bruto ($Y)"
- Al liquidar POR_DIA: validar que hay al menos un día pendiente
- Al eliminar vacación: ninguna restricción (es solo informativo)
