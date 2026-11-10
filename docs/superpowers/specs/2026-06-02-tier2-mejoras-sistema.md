# Spec: Tier 2 — Mejoras al sistema existente

**Fecha:** 2026-06-02  
**Rama:** `feature/tier2-mejoras-sistema` (desde `develop`)  
**Scope:** Tres mejoras independientes al sistema actual. Una sola rama, commits separados por mejora.

---

## Contexto

Tras completar el Tier 1 (UI pendiente), el sistema tiene todos los endpoints expuestos con interfaz. El Tier 2 mejora tres áreas operativas concretas identificadas al comparar el sistema contra el uso real del asadero:

1. **Denominaciones en cierre de caja** — el asadero cuenta los billetes por denominación al cerrar. El sistema actualmente solo recibe un total en COP; no hay trazabilidad del desglose.
2. **Umbral de alerta de stock** — el Dashboard muestra colores de alerta con valores hardcodeados (rojo < 2, ámbar < 5). El nivel de alarma relevante varía según el volumen de ventas del asadero y debería ser configurable.
3. **Comparativo mensual de inventario** — el asadero controla cuántos cuyes y gallinas consume por mes en un Excel (hoja "COMPARATIVO"). El sistema no tiene este reporte.

---

## Mejora 1: Denominaciones en cierre de caja

### Backend

**Contexto operativo:** El dueño coloca una "base" (ej. $350.000) en la caja al iniciar el día para dar cambio. Al cerrar, el operador cuenta TODO el efectivo físico (billetes + monedas + la base misma). La base se deja en la caja para el día siguiente — no es ingreso. El operador entrega al dueño: `total contado − base`.

**Entidad `CierreCaja`** — agregar 8 campos nuevos (todos nullable para compatibilidad con cierres históricos):

```java
@Column(name = "base_cop",       precision = 19, scale = 0) private BigDecimal baseCop;
@Column(name = "billetes_100k")  private Integer billetes100k;
@Column(name = "billetes_50k")   private Integer billetes50k;
@Column(name = "billetes_20k")   private Integer billetes20k;
@Column(name = "billetes_10k")   private Integer billetes10k;
@Column(name = "billetes_5k")    private Integer billetes5k;
@Column(name = "billetes_2k")    private Integer billetes2k;
@Column(name = "monedas_cop",    precision = 19, scale = 0) private BigDecimal monedasCop;
```

`baseCop` = monto que se deja en caja para el siguiente día (pre-cargado desde config, editable).

**`CierreCajaRequest`** — agregar los mismos 8 campos opcionales (`@Nullable`).

**`CierreCajaDTO`** — agregar los 8 campos para que el panel los muestre en modo lectura. Incluir campo calculado `paraEntregar` (BigDecimal) = `contadoCop - baseCop` (calculado en el mapper, null si no hay denominaciones).

**`CierreCajaServiceImpl.cerrar()`** — si el request tiene al menos un campo de denominación no nulo, calcular `contadoCop` como:

```java
BigDecimal contadoCop = safeDecimal(req.getBaseCop())
    .add(safe(req.getBilletes100k()).multiply(new BigDecimal("100000")))
    .add(safe(req.getBilletes50k()).multiply(new BigDecimal("50000")))
    .add(safe(req.getBilletes20k()).multiply(new BigDecimal("20000")))
    .add(safe(req.getBilletes10k()).multiply(new BigDecimal("10000")))
    .add(safe(req.getBilletes5k()).multiply(new BigDecimal("5000")))
    .add(safe(req.getBilletes2k()).multiply(new BigDecimal("2000")))
    .add(safeDecimal(req.getMonedasCop()));
// donde safe(Integer n) → BigDecimal, safeDecimal(BigDecimal) → BigDecimal o ZERO si null
```

Si todos los campos son nulos, usar `req.getContadoCop()` directamente (compatibilidad hacia atrás).

**`DataInitializer`** — agregar clave `base_caja_cop` con valor `"0"` si no existe (idempotente).

**Mapper `CierreCajaMapper`** — mapear los 8 campos nuevos en entity ↔ DTO. Calcular `paraEntregar = contadoCop - baseCop` cuando `baseCop != null`.

### Frontend

**`CierreCajaPanel.jsx`** — reemplazar el input único `contadoCop` por grilla de denominaciones con base:

```
Conteo de caja COP
┌─────────────────────┬───────────┬──────────────┐
│                     │ Cantidad  │ Subtotal     │
├─────────────────────┼───────────┼──────────────┤
│ Base para mañana    │ [350.000] │  $350.000    │  ← pre-cargada de config, editable
├─────────────────────┼───────────┼──────────────┤
│ $100.000            │ [____]    │       $0     │
│ $ 50.000            │ [____]    │       $0     │
│ $ 20.000            │ [____]    │       $0     │
│ $ 10.000            │ [____]    │       $0     │
│ $  5.000            │ [____]    │       $0     │
│ $  2.000            │ [____]    │       $0     │
│ Monedas (COP)       │ [______]  │       —      │  ← monto total en monedas
├─────────────────────┴───────────┼──────────────┤
│ Total contado                   │   $350.000   │
│ Para entregar al dueño          │         $0   │  ← total − base (informativo)
└─────────────────────────────────┴──────────────┘
```

- "Base para mañana" se pre-carga con `GET /api/v1/configuracion/base_caja_cop` al montar el panel
- Los subtotales y el total se calculan en tiempo real con `useMemo`
- "Para entregar" = total − base (informativo, no afecta la reconciliación financiera)
- El `contadoCop` enviado al backend = total calculado (base + billetes + monedas)
- Si el cierre ya existe (modo lectura), mostrar el desglose guardado
- Las denominaciones son **opcionales** — fallback: si todos quedan vacíos, campo manual `contadoCop`

---

## Mejora 2: Umbral de alerta de stock bajo

### Backend

**Entidad `Inventario`** — agregar campo `alertaMinima`:

```java
@Column(name = "alerta_minima", precision = 19, scale = 4)
private BigDecimal alertaMinima;
```

**`DataInitializer`** — nuevo método de backfill idempotente `backfillAlertasInventario()`:
```java
// Si alertaMinima es null para gallina → setear 5
// Si alertaMinima es null para cuy → setear 10
```

Llamar desde el método `run()` del DataInitializer.

**`InventarioDTO`** — agregar campo `alertaMinima BigDecimal`.

**`InventarioMapper`** — mapear el nuevo campo.

**`IInventarioService`** — nuevo método:
```java
InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima);
```

**`InventarioServiceImpl`** — implementar: buscar por insumo, setear `alertaMinima`, guardar, devolver DTO.

**`InventarioController`** — nuevo endpoint:
```
PATCH /api/v1/inventario/{insumo}/alerta
Body: { "alertaMinima": 8.0 }
Response: InventarioDTO
```

**`inventarioService.js` (frontend service)** — agregar método:
```js
actualizarAlerta: (insumo, alertaMinima) =>
  api.patch(`/inventario/${insumo}/alerta`, { alertaMinima }).then((r) => r.data),
```

### Frontend

**`Dashboard.jsx`** — reemplazar thresholds hardcodeados por `s.alertaMinima`:

```js
const umbral = s.alertaMinima ?? 5; // fallback si aún no tiene alerta configurada
const color =
  deficit > 0 || s.stockActual < umbral * 0.4
    ? "red-600"      // crítico
    : s.stockActual < umbral
    ? "amber-500"    // alerta
    : "verde-claro"; // saludable
```

La alerta global de stock crítico (texto rojo) se dispara cuando `s.stockActual < umbral * 0.4`.

**`InventarioTab.jsx`** — agregar columna "Alerta mínima" con edición inline:
- Input numérico (step=0.25) por fila, pre-rellenado con `alertaMinima`
- Botón guardar **explícito** por fila (`Save` de lucide-react) → llama `inventarioService.actualizarAlerta(insumo, valor)` → invalida `["inventario"]`
- `queryKey: ["inventario"]` ya existe — se invalida igual

---

## Mejora 3: Comparativo mensual de inventario

### Backend

**Nuevo DTO `ComparativoMensualDTO`:**
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ComparativoMensualDTO {
    private Integer año;
    private List<FilaMes> meses;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FilaMes {
        private Integer mes;      // 1-12
        private BigDecimal gallina; // suma consumida ese mes
        private BigDecimal cuy;
    }
}
```

**`IInventarioService`** — nuevo método:
```java
ComparativoMensualDTO comparativoAnual(int año);
```

**`InventarioServiceImpl`** — implementar con query nativa SQLite:
```sql
SELECT insumo,
       CAST(strftime('%m', registrado_en) AS INTEGER) AS mes,
       SUM(cantidad) AS total
FROM movimiento_inventario
WHERE tipo = 'salida'
  AND referencia_tipo = 'pedido'
  AND strftime('%Y', registrado_en) = :año
GROUP BY insumo, strftime('%m', registrado_en)
ORDER BY mes
```

Usar `@Query(value = "...", nativeQuery = true)` en un nuevo método del `MovimientoInventarioRepository`.

Procesar resultado: construir 12 filas (una por mes, BigDecimal.ZERO si no hay datos). Si el insumo no está en el resultado de un mes, usar ZERO.

**`InventarioController`** — nuevo endpoint:
```
GET /api/v1/inventario/comparativo?año=2025
Response: ComparativoMensualDTO
```

`año` default: año actual si no se pasa.

**`inventarioService.js` (frontend)** — agregar:
```js
comparativo: (año) =>
  api.get("/inventario/comparativo", { params: { año } }).then((r) => r.data),
```

### Frontend

**Nuevo tab "Reportes" (10° tab) en `ConfiguracionPage`:**
```js
{ id: "reportes", label: "Reportes" },
```

**Nuevo archivo `ReportesTab.jsx`:**
- State: `año` (default: `new Date().getFullYear()`)
- `useQuery({ queryKey: ["comparativo", año], queryFn: () => inventarioService.comparativo(año) })`
- Selector de año: botones `< año >` (anterior/siguiente)
- Tabla:

| Mes | Gallinas consumidas | Cuyes consumidos |
|-----|---------------------|-----------------|
| Enero | 150.50 | 230.00 |
| ... | ... | ... |
| **Total** | **1.234,00** | **2.100,00** |

- Meses con cero se muestran con "—" en ambas columnas
- Usar `formatDecimal()` de `utils/format.js` para los valores

---

## Archivos a crear (3)

| # | Ruta | Descripción |
|---|---|---|
| 1 | `Frontend/src/pages/configuracion/tabs/ReportesTab.jsx` | Tab comparativo mensual |
| 2 | `Backend/.../dto/ComparativoMensualDTO.java` | DTO para el comparativo |
| 3 | `Backend/.../dto/ComparativoMensualDTO.java` (ya listado arriba) | — |

## Archivos a modificar (backend — 8)

| Archivo | Cambio |
|---|---|
| `CierreCaja.java` | +8 campos (baseCop + 7 denominaciones) |
| `CierreCajaRequest.java` | +8 campos opcionales |
| `CierreCajaDTO.java` | +8 campos + campo calculado `paraEntregar` |
| `CierreCajaServiceImpl.java` | calcular contadoCop desde base + denominaciones |
| `CierreCajaMapper.java` | mapear campos nuevos + calcular paraEntregar |
| `DataInitializer.java` | seed `base_caja_cop = "0"` en ConfiguracionGeneral |
| `Inventario.java` | +campo alertaMinima |
| `InventarioDTO.java` | +campo alertaMinima |
| `InventarioMapper.java` | mapear alertaMinima |
| `IInventarioService.java` | +2 métodos nuevos |
| `InventarioServiceImpl.java` | implementar los 2 métodos |
| `InventarioController.java` | +2 endpoints nuevos |
| `DataInitializer.java` | backfillAlertasInventario() |
| `MovimientoInventarioRepository.java` | +query nativa para comparativo |

## Archivos a modificar (frontend — 4)

| Archivo | Cambio |
|---|---|
| `CierreCajaPanel.jsx` | reemplazar input único por grilla denominaciones |
| `Dashboard.jsx` | thresholds dinámicos desde alertaMinima |
| `InventarioTab.jsx` | columna alerta mínima editable |
| `ConfiguracionPage.jsx` | +tab Reportes |
| `inventarioService.js` | +actualizarAlerta, +comparativo |

---

## Verificación end-to-end

1. **Denominaciones:**
   - Ir a `/resumen` → panel cierre → ingresar conteo de billetes → verificar que el total COP se calcula en tiempo real
   - Ejecutar cierre → verificar que el registro en BD tiene los campos de denominación
   - Abrir un cierre existente anterior → verificar que muestra solo total (campos null = retrocompatible)

2. **Umbral de alerta:**
   - Arrancar backend con DataInitializer → verificar que gallina=5 y cuy=10 en BD
   - Ir a `/configuracion` → tab Inventario → cambiar alerta de gallina a 3 → guardar
   - Ir a Dashboard → verificar que el color de gallina cambia según el nuevo umbral
   - Poner stock de gallina en 1 → verificar que aparece rojo (< 40% de 3 = < 1.2)

3. **Comparativo:**
   - Ir a `/configuracion` → tab Reportes → verificar que carga el año actual
   - Cambiar año → verificar que la tabla cambia
   - Ejecutar varios pedidos con cuyes y gallinas, finalizarlos → verificar que el mes aparece en la tabla
   - `mvn test` — tests existentes en verde
