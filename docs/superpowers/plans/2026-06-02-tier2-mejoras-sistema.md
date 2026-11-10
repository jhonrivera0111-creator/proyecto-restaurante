# Tier 2 — Mejoras al sistema existente

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tres mejoras al sistema existente: desglose de denominaciones de billetes en el cierre de caja (con base para el día siguiente), umbral de alerta de stock configurable por insumo, y reporte de comparativo mensual de consumo de cuyes y gallinas.

**Architecture:** Backend-first en los tres casos — cada mejora agrega campos o endpoints al backend y luego actualiza el frontend que los consume. No hay módulos nuevos; son extensiones de los existentes. Una sola rama `feature/tier2-mejoras-sistema`.

**Tech Stack:** Java 17 + Spring Boot 3.4 + Spring Data JPA + MapStruct + Lombok + SQLite (backend); React 19 + TanStack Query v5 + Axios + lucide-react (frontend). Tests: mvn test (backend), vitest (frontend).

---

## Mejora 1: Denominaciones en cierre de caja

**Contexto operativo:** El dueño coloca una "base" (ej. $350.000) en la caja al iniciar el día para dar cambio. Al cerrar, el operador cuenta TODO el físico (base + billetes + monedas). La base se deja para el siguiente día; lo demás se entrega al dueño.

---

### Task 1: Extender entidad CierreCaja con denominaciones + base

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/CierreCaja.java`

- [ ] **Step 1: Agregar 8 campos nullable al final de CierreCaja.java**

Abrir el archivo. Antes del último `}` de la clase, insertar:

```java
    // ── Denominaciones arqueo COP ──────────────────────────────────────────
    @Column(name = "base_cop", precision = 19, scale = 0)
    private BigDecimal baseCop;          // monto reservado para el día siguiente

    @Column(name = "billetes_100k")
    private Integer billetes100k;

    @Column(name = "billetes_50k")
    private Integer billetes50k;

    @Column(name = "billetes_20k")
    private Integer billetes20k;

    @Column(name = "billetes_10k")
    private Integer billetes10k;

    @Column(name = "billetes_5k")
    private Integer billetes5k;

    @Column(name = "billetes_2k")
    private Integer billetes2k;

    @Column(name = "monedas_cop", precision = 19, scale = 0)
    private BigDecimal monedasCop;       // valor total de monedas en COP
```

Todos nullable — cierres históricos sin denominaciones siguen siendo válidos.

- [ ] **Step 2: Verificar que compila**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS sin errores.

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/CierreCaja.java
git commit -m "feat(cierre-caja): agregar campos de denominaciones y base al cierre"
```

---

### Task 2: Extender CierreCajaRequest y CierreCajaDTO

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CierreCajaRequest.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CierreCajaDTO.java`

- [ ] **Step 1: Agregar campos al CierreCajaRequest**

El archivo actual tiene: `fecha, usuarioId, contadoCop, contadoUsd, contadoNequi, observacion`.

Agregar al final de la clase (antes del `}`):

```java
    // Denominaciones opcionales — si se envían, contadoCop se calcula desde ellas
    private BigDecimal baseCop;
    private Integer billetes100k;
    private Integer billetes50k;
    private Integer billetes20k;
    private Integer billetes10k;
    private Integer billetes5k;
    private Integer billetes2k;
    private BigDecimal monedasCop;
```

- [ ] **Step 2: Agregar campos al CierreCajaDTO**

El archivo actual tiene los campos de esperado/contado/diferencia. Agregar al final (antes del `}`):

```java
    // Denominaciones del arqueo (null en cierres históricos)
    private BigDecimal baseCop;
    private Integer billetes100k;
    private Integer billetes50k;
    private Integer billetes20k;
    private Integer billetes10k;
    private Integer billetes5k;
    private Integer billetes2k;
    private BigDecimal monedasCop;
    private BigDecimal paraEntregar;   // calculado: contadoCop - baseCop
```

- [ ] **Step 3: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CierreCajaRequest.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/CierreCajaDTO.java
git commit -m "feat(cierre-caja): agregar denominaciones a Request y DTO"
```

---

### Task 3: Actualizar CierreCajaMapper

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/CierreCajaMapper.java`

- [ ] **Step 1: Agregar método default para calcular paraEntregar**

El mapper actual es:
```java
@Mapper(componentModel = "spring")
public interface CierreCajaMapper {
    CierreCajaDTO toDTO(CierreCaja c);
}
```

Reemplazar con:

```java
package com.casaverde.backend.fachadaServices.mapper;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CierreCajaMapper {

    CierreCajaDTO toDTO(CierreCaja c);

    @AfterMapping
    default void calcularParaEntregar(CierreCaja c, @MappingTarget CierreCajaDTO dto) {
        if (c.getBaseCop() != null && c.getContadoCop() != null) {
            dto.setParaEntregar(c.getContadoCop().subtract(c.getBaseCop()));
        }
    }
}
```

MapStruct mapea automáticamente los campos con el mismo nombre (los 8 campos de denominación). `@AfterMapping` calcula `paraEntregar` después del mapeo automático.

- [ ] **Step 2: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/mapper/CierreCajaMapper.java
git commit -m "feat(cierre-caja): calcular paraEntregar en mapper con @AfterMapping"
```

---

### Task 4: Actualizar CierreCajaServiceImpl — calcular contadoCop desde denominaciones

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/CierreCajaServiceImpl.java`

- [ ] **Step 1: Agregar métodos auxiliares privados al final de la clase**

Antes del último `}` de `CierreCajaServiceImpl`, agregar:

```java
    private BigDecimal denom(Integer cantidad, long valorUnitario) {
        if (cantidad == null || cantidad == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cantidad).multiply(BigDecimal.valueOf(valorUnitario));
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private boolean tieneDenominaciones(CierreCajaRequest req) {
        return req.getBaseCop() != null
            || req.getBilletes100k() != null
            || req.getBilletes50k() != null
            || req.getBilletes20k() != null
            || req.getBilletes10k() != null
            || req.getBilletes5k() != null
            || req.getBilletes2k() != null
            || req.getMonedasCop() != null;
    }

    private BigDecimal calcularContadoCopDesdeDenominaciones(CierreCajaRequest req) {
        return safe(req.getBaseCop())
            .add(denom(req.getBilletes100k(), 100_000L))
            .add(denom(req.getBilletes50k(),   50_000L))
            .add(denom(req.getBilletes20k(),   20_000L))
            .add(denom(req.getBilletes10k(),   10_000L))
            .add(denom(req.getBilletes5k(),     5_000L))
            .add(denom(req.getBilletes2k(),     2_000L))
            .add(safe(req.getMonedasCop()));
    }
```

- [ ] **Step 2: Calcular contadoCop antes de la línea que llama a normalizarOCero**

En el método `cerrar()`, localizar la línea:
```java
BigDecimal contadoCop = normalizarOCero("COP", request.getContadoCop(), esperadoCop);
```

Reemplazar con:

```java
        if (tieneDenominaciones(request)) {
            request.setContadoCop(calcularContadoCopDesdeDenominaciones(request));
        }
        BigDecimal contadoCop = normalizarOCero("COP", request.getContadoCop(), esperadoCop);
```

- [ ] **Step 3: Guardar los campos de denominación en el builder del cierre**

En el builder `.build()` dentro de `cerrar()`, después de `.observacion(request.getObservacion())`, agregar:

```java
                .baseCop(request.getBaseCop())
                .billetes100k(request.getBilletes100k())
                .billetes50k(request.getBilletes50k())
                .billetes20k(request.getBilletes20k())
                .billetes10k(request.getBilletes10k())
                .billetes5k(request.getBilletes5k())
                .billetes2k(request.getBilletes2k())
                .monedasCop(request.getMonedasCop())
```

- [ ] **Step 4: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 5: Seed base_caja_cop en DataInitializer**

Abrir `Backend/src/main/java/com/casaverde/backend/config/DataInitializer.java`.

Dentro del método `run()`, después de las líneas que crean otras claves de ConfiguracionGeneral, agregar:

```java
        configRepo.findById("base_caja_cop").orElseGet(() -> configRepo.save(
            ConfiguracionGeneral.builder().clave("base_caja_cop").valor("0").build()));
```

- [ ] **Step 6: Compilar y correr tests backend**

```bash
cd Backend && mvn test -q
```

Esperado: BUILD SUCCESS, todos los tests en verde.

- [ ] **Step 7: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/pedidoService/CierreCajaServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/config/DataInitializer.java
git commit -m "feat(cierre-caja): calcular contadoCop desde denominaciones en el servicio"
```

---

### Task 5: Actualizar CierreCajaPanel.jsx — grilla de denominaciones

**Files:**
- Modify: `Frontend/src/components/resumen/CierreCajaPanel.jsx`

- [ ] **Step 1: Leer el archivo actual completo**

```bash
cat Frontend/src/components/resumen/CierreCajaPanel.jsx
```

Identificar: el campo `contadoCop` (el único input de efectivo actual) y cómo se construye el payload de `cerrarMut`.

- [ ] **Step 2: Agregar estado de denominaciones**

En el componente, agregar un estado para las denominaciones. Localizar los `useState` existentes y agregar:

```jsx
const [denoms, setDenoms] = useState({
  baseCop: "",
  billetes100k: "",
  billetes50k: "",
  billetes20k: "",
  billetes10k: "",
  billetes5k: "",
  billetes2k: "",
  monedasCop: "",
});
```

- [ ] **Step 3: Agregar método get(clave) a configuracionService.js**

El archivo `Frontend/src/services/configuracionService.js` actualmente tiene `listar()` y `actualizarValor()`. El backend expone `GET /api/v1/configuracion/{clave}`.

Agregar en el objeto `configuracionService`:

```js
  get: (clave) => api.get(`/configuracion/${clave}`).then((r) => r.data),
```

- [ ] **Step 4: Cargar base desde config en CierreCajaPanel**

En los imports de CierreCajaPanel agregar:
```jsx
import { configuracionService } from "../../services/configuracionService";
```

Dentro del componente (junto a los otros `useQuery` existentes):

```jsx
const { data: baseCfg } = useQuery({
  queryKey: ["config", "base_caja_cop"],
  queryFn: () => configuracionService.get("base_caja_cop"),
  staleTime: 300_000,
});

// Pre-cargar baseCop cuando llega el valor de config (solo si aún no se ingresó nada)
useEffect(() => {
  if (baseCfg?.valor && denoms.baseCop === "") {
    setDenoms((prev) => ({ ...prev, baseCop: baseCfg.valor }));
  }
}, [baseCfg]);
```

- [ ] **Step 4: Calcular total en tiempo real con useMemo**

```jsx
const totalContadoCop = useMemo(() => {
  const base = parseFloat(denoms.baseCop) || 0;
  const b100 = (parseInt(denoms.billetes100k) || 0) * 100_000;
  const b50  = (parseInt(denoms.billetes50k)  || 0) * 50_000;
  const b20  = (parseInt(denoms.billetes20k)  || 0) * 20_000;
  const b10  = (parseInt(denoms.billetes10k)  || 0) * 10_000;
  const b5   = (parseInt(denoms.billetes5k)   || 0) * 5_000;
  const b2   = (parseInt(denoms.billetes2k)   || 0) * 2_000;
  const mon  = parseFloat(denoms.monedasCop)  || 0;
  return base + b100 + b50 + b20 + b10 + b5 + b2 + mon;
}, [denoms]);

const paraEntregar = useMemo(() => {
  const base = parseFloat(denoms.baseCop) || 0;
  return totalContadoCop - base;
}, [totalContadoCop, denoms.baseCop]);
```

- [ ] **Step 5: Reemplazar el input de contadoCop por la grilla de denominaciones**

En el JSX, localizar el input de `contadoCop` para COP y reemplazarlo con:

```jsx
{/* Grilla de denominaciones COP */}
<div className="mt-3">
  <p className="text-sm font-semibold text-verde-oscuro mb-2">Conteo de caja COP</p>
  <table className="w-full text-sm">
    <thead>
      <tr className="text-gray-500 text-xs">
        <th className="text-left pb-1">Denominación</th>
        <th className="text-center pb-1 w-24">Cantidad</th>
        <th className="text-right pb-1 w-28">Subtotal</th>
      </tr>
    </thead>
    <tbody>
      <tr className="border-b border-dashed">
        <td className="py-1 font-medium text-verde-oscuro">Base para mañana</td>
        <td className="py-1 text-center">
          <input
            type="number" min="0" step="1000"
            className="form-control text-right w-24"
            value={denoms.baseCop}
            onChange={(e) => setDenoms((p) => ({ ...p, baseCop: e.target.value }))}
          />
        </td>
        <td className="py-1 text-right">{formatCOP(parseFloat(denoms.baseCop) || 0)}</td>
      </tr>
      {[
        { key: "billetes100k", label: "$100.000", val: 100_000 },
        { key: "billetes50k",  label: "$ 50.000", val: 50_000  },
        { key: "billetes20k",  label: "$ 20.000", val: 20_000  },
        { key: "billetes10k",  label: "$ 10.000", val: 10_000  },
        { key: "billetes5k",   label: "$  5.000", val: 5_000   },
        { key: "billetes2k",   label: "$  2.000", val: 2_000   },
      ].map(({ key, label, val }) => (
        <tr key={key} className="border-b border-gray-100">
          <td className="py-1">{label}</td>
          <td className="py-1 text-center">
            <input
              type="number" min="0" step="1"
              className="form-control text-right w-24"
              value={denoms[key]}
              onChange={(e) => setDenoms((p) => ({ ...p, [key]: e.target.value }))}
            />
          </td>
          <td className="py-1 text-right">
            {formatCOP((parseInt(denoms[key]) || 0) * val)}
          </td>
        </tr>
      ))}
      <tr className="border-b border-gray-100">
        <td className="py-1">Monedas (valor total)</td>
        <td className="py-1 text-center">
          <input
            type="number" min="0" step="100"
            className="form-control text-right w-24"
            value={denoms.monedasCop}
            onChange={(e) => setDenoms((p) => ({ ...p, monedasCop: e.target.value }))}
          />
        </td>
        <td className="py-1 text-right">{formatCOP(parseFloat(denoms.monedasCop) || 0)}</td>
      </tr>
    </tbody>
    <tfoot>
      <tr className="font-semibold border-t-2">
        <td colSpan={2} className="pt-2">Total contado COP</td>
        <td className="pt-2 text-right">{formatCOP(totalContadoCop)}</td>
      </tr>
      <tr className="text-sm text-gray-600">
        <td colSpan={2}>Para entregar al dueño</td>
        <td className="text-right">{formatCOP(paraEntregar)}</td>
      </tr>
    </tfoot>
  </table>
</div>
```

- [ ] **Step 6: Actualizar el payload que se envía al cerrar**

Localizar dónde `cerrarMut` llama al servicio con el payload. Incluir los campos de denominación:

```jsx
// Donde se construye el payload para cerrar:
const payload = {
  fecha,
  usuarioId: usuario?.id,
  contadoUsd: parseFloat(contadoUsd) || 0,
  contadoNequi: parseFloat(contadoNequi) || 0,
  observacion,
  // Denominaciones COP (el backend recalcula contadoCop)
  baseCop:      parseFloat(denoms.baseCop)      || null,
  billetes100k: parseInt(denoms.billetes100k)   || null,
  billetes50k:  parseInt(denoms.billetes50k)    || null,
  billetes20k:  parseInt(denoms.billetes20k)    || null,
  billetes10k:  parseInt(denoms.billetes10k)    || null,
  billetes5k:   parseInt(denoms.billetes5k)     || null,
  billetes2k:   parseInt(denoms.billetes2k)     || null,
  monedasCop:   parseFloat(denoms.monedasCop)   || null,
  // contadoCop solo como fallback si no se ingresan denominaciones
  contadoCop: totalContadoCop || undefined,
};
```

- [ ] **Step 8: Mostrar denominaciones guardadas en modo lectura (cierre ya existente)**

Si el cierre del día ya fue ejecutado, CierreCajaPanel muestra un resumen de solo lectura. Localizar el bloque donde se muestra `cierre.contadoCop` (modo lectura) y reemplazarlo con:

```jsx
{cierre.billetes100k != null ? (
  <div className="text-sm mt-2 space-y-0.5">
    <p className="font-medium text-verde-oscuro mb-1">Arqueo registrado:</p>
    {cierre.baseCop > 0 && <p>Base para mañana: {formatCOP(cierre.baseCop)}</p>}
    {cierre.billetes100k > 0 && <p>$100.000 × {cierre.billetes100k} = {formatCOP(cierre.billetes100k * 100_000)}</p>}
    {cierre.billetes50k  > 0 && <p>$ 50.000 × {cierre.billetes50k}  = {formatCOP(cierre.billetes50k  * 50_000)}</p>}
    {cierre.billetes20k  > 0 && <p>$ 20.000 × {cierre.billetes20k}  = {formatCOP(cierre.billetes20k  * 20_000)}</p>}
    {cierre.billetes10k  > 0 && <p>$ 10.000 × {cierre.billetes10k}  = {formatCOP(cierre.billetes10k  * 10_000)}</p>}
    {cierre.billetes5k   > 0 && <p>$  5.000 × {cierre.billetes5k}   = {formatCOP(cierre.billetes5k   * 5_000)}</p>}
    {cierre.billetes2k   > 0 && <p>$  2.000 × {cierre.billetes2k}   = {formatCOP(cierre.billetes2k   * 2_000)}</p>}
    {cierre.monedasCop   > 0 && <p>Monedas: {formatCOP(cierre.monedasCop)}</p>}
    <p className="font-semibold border-t pt-1 mt-1">Total: {formatCOP(cierre.contadoCop)}</p>
    {cierre.paraEntregar != null && (
      <p className="text-verde-oscuro font-semibold">Para entregar: {formatCOP(cierre.paraEntregar)}</p>
    )}
  </div>
) : (
  <p className="text-sm">Total contado COP: <strong>{formatCOP(cierre.contadoCop)}</strong></p>
)}
```

- [ ] **Step 8: Correr tests frontend**

```bash
cd Frontend && npx vitest run --reporter=dot 2>&1 | tail -5
```

Esperado: todos los tests en verde (los existentes no tocan CierreCajaPanel).

- [ ] **Step 9: Correr tests frontend**

```bash
cd Frontend && npx vitest run --reporter=dot 2>&1 | tail -5
```

Esperado: todos en verde.

- [ ] **Step 10: Commit**

```bash
git add Frontend/src/services/configuracionService.js \
        Frontend/src/components/resumen/CierreCajaPanel.jsx
git commit -m "feat(cierre-caja): grilla de denominaciones y base en CierreCajaPanel"
```

---

## Mejora 2: Umbral de alerta de stock bajo

---

### Task 6: Agregar alertaMinima a la entidad Inventario y al DTO

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Inventario.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/InventarioDTO.java`

- [ ] **Step 1: Agregar campo alertaMinima a Inventario.java**

Inventario.java tiene: `id, version, insumo, stockActual, unidad, actualizadoEn`.

Agregar después de `stockActual`:

```java
    @Column(name = "alerta_minima", precision = 19, scale = 4)
    private BigDecimal alertaMinima;
```

- [ ] **Step 2: Agregar alertaMinima al InventarioDTO.java**

InventarioDTO.java tiene: `id, insumo, stockActual, unidad, actualizadoEn`.

Agregar después de `stockActual`:

```java
    private BigDecimal alertaMinima;
```

- [ ] **Step 3: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS. MapStruct mapea `alertaMinima` automáticamente (mismo nombre en entidad y DTO).

- [ ] **Step 4: Backfill en DataInitializer**

Abrir DataInitializer. Añadir método de backfill:

```java
private void backfillAlertasInventario() {
    inventarioRepo.findAll().forEach(inv -> {
        if (inv.getAlertaMinima() == null) {
            if ("gallina".equalsIgnoreCase(inv.getInsumo())) {
                inv.setAlertaMinima(new BigDecimal("5"));
            } else if ("cuy".equalsIgnoreCase(inv.getInsumo())) {
                inv.setAlertaMinima(new BigDecimal("10"));
            }
            inventarioRepo.save(inv);
        }
    });
}
```

Y llamarlo al final del método `run()`:

```java
backfillAlertasInventario();
```

Verificar que `inventarioRepo` ya está inyectado (buscar `InventarioRepository` en los campos del DataInitializer). Si no existe, agregarlo como `private final InventarioRepository inventarioRepo;`.

- [ ] **Step 5: Correr tests backend**

```bash
cd Backend && mvn test -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Inventario.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/InventarioDTO.java \
        Backend/src/main/java/com/casaverde/backend/config/DataInitializer.java
git commit -m "feat(inventario): agregar alertaMinima con backfill gallina=5, cuy=10"
```

---

### Task 7: Endpoint PATCH /inventario/{insumo}/alerta

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaControladores/InventarioController.java`

- [ ] **Step 1: Agregar método a IInventarioService**

En la interfaz `IInventarioService.java`, agregar al final (antes del `}`):

```java
    InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima);
```

- [ ] **Step 2: Implementar en InventarioServiceImpl**

Abrir `InventarioServiceImpl.java`. Agregar método:

```java
    @Transactional
    @Override
    public InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima) {
        if (alertaMinima == null || alertaMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("La alerta mínima debe ser un valor positivo");
        }
        Inventario inv = inventarioRepo.findByInsumo(insumo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + insumo));
        inv.setAlertaMinima(alertaMinima);
        return inventarioMapper.toDTO(inventarioRepo.save(inv));
    }
```

Verificar que `inventarioRepo.findByInsumo(String)` existe en el repository. Si no existe, agregarlo:

En `InventarioRepository.java`:
```java
Optional<Inventario> findByInsumo(String insumo);
```

- [ ] **Step 3: Agregar endpoint al InventarioController**

En `InventarioController.java`, agregar import de `BigDecimal` y el endpoint:

```java
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PatchMapping;
import java.util.Map;

// Dentro de la clase:
@PatchMapping("/{insumo}/alerta")
public InventarioDTO actualizarAlerta(
        @PathVariable String insumo,
        @RequestBody Map<String, BigDecimal> body) {
    return service.actualizarAlerta(insumo, body.get("alertaMinima"));
}
```

- [ ] **Step 4: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 5: Correr tests backend**

```bash
cd Backend && mvn test -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/capaControladores/InventarioController.java
git commit -m "feat(inventario): endpoint PATCH /inventario/{insumo}/alerta"
```

---

### Task 8: Frontend — alertaMinima en Dashboard e InventarioTab

**Files:**
- Modify: `Frontend/src/services/inventarioService.js`
- Modify: `Frontend/src/pages/dashboard/Dashboard.jsx`
- Modify: `Frontend/src/pages/configuracion/tabs/InventarioTab.jsx`

- [ ] **Step 1: Agregar actualizarAlerta al inventarioService.js**

El archivo actual tiene: `listarStock`, `listarMovimientos`, `registrarMovimiento`.

Agregar:

```js
  actualizarAlerta: (insumo, alertaMinima) =>
    api.patch(`/inventario/${insumo}/alerta`, { alertaMinima }).then((r) => r.data),
```

- [ ] **Step 2: Actualizar thresholds en Dashboard.jsx**

Buscar en Dashboard.jsx la función o lógica que determina el color del stock. El código actual es:

```js
if (deficit > 0 || s.stockActual < 2) {
  color = "red-600"
} else if (s.stockActual < 5) {
  color = "amber-500"
} else {
  color = "verde-claro"
}
```

Reemplazar con:

```js
const umbral = s.alertaMinima ?? 5;
const color =
  deficit > 0 || s.stockActual < umbral * 0.4
    ? "red-600"
    : s.stockActual < umbral
    ? "amber-500"
    : "verde-claro";
```

Buscar también la condición de alerta global (el badge rojo que aparece cuando hay stock crítico). La condición actual es `s.stockActual < 2`. Reemplazar con `s.stockActual < (s.alertaMinima ?? 5) * 0.4`.

- [ ] **Step 3: Agregar columna "Alerta mínima" en InventarioTab.jsx**

El componente `InsumoRow` actual muestra: nombre, stock actual, input de ajuste de stock.

Agregar un segundo form/input para editar `alertaMinima`. Dentro de `InsumoRow`, después del div del nombre/stock, agregar:

```jsx
function InsumoRow({ insumo, onAjustado }) {
  // Estado existente: cantidad, msg, confirmando
  // Nuevo estado:
  const [alerta, setAlerta] = useState(
    insumo.alertaMinima != null ? String(insumo.alertaMinima) : ""
  );
  const [alertaMsg, setAlertaMsg] = useState(null);

  const mutAlerta = useMutation({
    mutationFn: () =>
      inventarioService.actualizarAlerta(insumo.insumo, parseFloat(alerta)),
    onSuccess: () => {
      setAlertaMsg({ tipo: "ok", texto: "Guardado" });
      onAjustado();
      setTimeout(() => setAlertaMsg(null), 2000);
    },
    onError: (err) => setAlertaMsg({ tipo: "error", texto: err.message }),
  });
  // ... resto del componente existente ...
```

En el JSX de `InsumoRow`, agregar después del form de ajuste:

```jsx
  {/* Alerta mínima */}
  <div className="flex items-center gap-2 ml-4 border-l pl-4">
    <label className="text-xs text-gray-500 whitespace-nowrap">Alerta mínima:</label>
    <input
      type="number" min="0" step="0.25"
      className="form-control w-20"
      value={alerta}
      onChange={(e) => { setAlerta(e.target.value); setAlertaMsg(null); }}
      aria-label={`Alerta mínima de ${insumo.insumo}`}
    />
    <button
      type="button"
      className="btn btn-secondary btn-sm"
      onClick={() => mutAlerta.mutate()}
      disabled={mutAlerta.isPending}
    >
      <Save size={13} />
    </button>
    {alertaMsg && (
      <span className={`text-xs ${alertaMsg.tipo === "ok" ? "text-verde-claro" : "text-red-600"}`}>
        {alertaMsg.texto}
      </span>
    )}
  </div>
```

Agregar `import { Save } from "lucide-react"` si no está.

- [ ] **Step 4: Correr tests frontend**

```bash
cd Frontend && npx vitest run --reporter=dot 2>&1 | tail -5
```

Esperado: todos en verde.

- [ ] **Step 5: Commit**

```bash
git add Frontend/src/services/inventarioService.js \
        Frontend/src/pages/dashboard/Dashboard.jsx \
        Frontend/src/pages/configuracion/tabs/InventarioTab.jsx
git commit -m "feat(inventario): alertaMinima en Dashboard y InventarioTab"
```

---

## Mejora 3: Comparativo mensual de inventario

---

### Task 9: DTO + Repository query nativa

**Files:**
- Create: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ComparativoMensualDTO.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/MovimientoInventarioRepository.java`

- [ ] **Step 1: Crear ComparativoMensualDTO.java**

```java
package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparativoMensualDTO {

    private Integer año;
    private List<FilaMes> meses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaMes {
        private Integer mes;        // 1–12
        private BigDecimal gallina; // unidades consumidas ese mes (solo ventas finalizadas)
        private BigDecimal cuy;
    }
}
```

- [ ] **Step 2: Agregar query nativa al MovimientoInventarioRepository**

Agregar imports necesarios y el método al final del repository:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Dentro de la interfaz:
@Query(value = """
    SELECT insumo,
           CAST(strftime('%m', registrado_en) AS INTEGER) AS mes,
           SUM(cantidad) AS total
    FROM movimiento_inventario
    WHERE tipo = 'salida'
      AND referencia_tipo = 'pedido'
      AND strftime('%Y', registrado_en) = :año
    GROUP BY insumo, strftime('%m', registrado_en)
    ORDER BY mes
    """, nativeQuery = true)
List<Object[]> sumaSalidasPorMesYInsumo(@Param("año") String año);
```

El resultado es una lista de Object[] con: [0]=insumo (String), [1]=mes (Integer), [2]=total (BigDecimal).

- [ ] **Step 3: Compilar**

```bash
cd Backend && mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ComparativoMensualDTO.java \
        Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/MovimientoInventarioRepository.java
git commit -m "feat(comparativo): DTO y query nativa SQLite para comparativo mensual"
```

---

### Task 10: Servicio y endpoint del comparativo

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaControladores/InventarioController.java`

- [ ] **Step 1: Agregar método a IInventarioService**

```java
import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;

// Al final de la interfaz:
ComparativoMensualDTO comparativoAnual(int año);
```

- [ ] **Step 2: Implementar en InventarioServiceImpl**

Agregar método:

```java
    @Transactional(readOnly = true)
    @Override
    public ComparativoMensualDTO comparativoAnual(int año) {
        List<Object[]> rows = movimientoRepo.sumaSalidasPorMesYInsumo(String.valueOf(año));

        // Inicializar mapa [mes][insumo] → total
        Map<Integer, Map<String, BigDecimal>> mapa = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            mapa.put(m, new HashMap<>());
        }

        for (Object[] row : rows) {
            String insumo = (String) row[0];
            int mes = ((Number) row[1]).intValue();
            BigDecimal total = new BigDecimal(row[2].toString());
            mapa.get(mes).put(insumo.toLowerCase(), total);
        }

        List<ComparativoMensualDTO.FilaMes> meses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, BigDecimal> fila = mapa.get(m);
            meses.add(ComparativoMensualDTO.FilaMes.builder()
                .mes(m)
                .gallina(fila.getOrDefault("gallina", BigDecimal.ZERO))
                .cuy(fila.getOrDefault("cuy", BigDecimal.ZERO))
                .build());
        }

        return ComparativoMensualDTO.builder().año(año).meses(meses).build();
    }
```

Agregar imports necesarios: `Map, HashMap, ArrayList, List`.

Asegurarse de que `movimientoRepo` (MovimientoInventarioRepository) ya está inyectado en `InventarioServiceImpl`. Si no, agregarlo como campo.

- [ ] **Step 3: Endpoint en InventarioController**

```java
import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;
import java.time.LocalDate;

// Dentro de la clase (agregar antes del cierre):
@GetMapping("/comparativo")
public ComparativoMensualDTO comparativoAnual(
        @RequestParam(required = false) Integer año) {
    int anio = año != null ? año : LocalDate.now().getYear();
    return service.comparativoAnual(anio);
}
```

- [ ] **Step 4: Compilar y correr tests**

```bash
cd Backend && mvn test -q
```

Esperado: BUILD SUCCESS, todos en verde.

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IInventarioService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/InventarioServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/capaControladores/InventarioController.java
git commit -m "feat(comparativo): endpoint GET /inventario/comparativo con lógica de servicio"
```

---

### Task 11: Frontend — ReportesTab + ConfiguracionPage

**Files:**
- Modify: `Frontend/src/services/inventarioService.js`
- Create: `Frontend/src/pages/configuracion/tabs/ReportesTab.jsx`
- Modify: `Frontend/src/pages/configuracion/ConfiguracionPage.jsx`

- [ ] **Step 1: Agregar comparativo al inventarioService.js**

Agregar al objeto `inventarioService`:

```js
  comparativo: (año) =>
    api.get("/inventario/comparativo", { params: { año } }).then((r) => r.data),
```

- [ ] **Step 2: Crear ReportesTab.jsx**

```jsx
import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { inventarioService } from "../../../services/inventarioService";
import { formatDecimal } from "../../../utils/format";

const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio",
                "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

export default function ReportesTab() {
  const [año, setAño] = useState(new Date().getFullYear());

  const { data, isLoading } = useQuery({
    queryKey: ["comparativo", año],
    queryFn: () => inventarioService.comparativo(año),
    staleTime: 60_000,
  });

  const meses = data?.meses ?? [];

  const totales = useMemo(() => ({
    gallina: meses.reduce((s, m) => s + parseFloat(m.gallina ?? 0), 0),
    cuy:     meses.reduce((s, m) => s + parseFloat(m.cuy ?? 0), 0),
  }), [meses]);

  return (
    <div>
      {/* Selector de año */}
      <div className="toolbar">
        <div className="flex items-center gap-2">
          <button className="btn btn-secondary btn-sm btn-icon"
                  onClick={() => setAño((a) => a - 1)}>
            <ChevronLeft size={16} />
          </button>
          <span className="font-semibold text-verde-oscuro w-14 text-center">{año}</span>
          <button className="btn btn-secondary btn-sm btn-icon"
                  onClick={() => setAño((a) => a + 1)}
                  disabled={año >= new Date().getFullYear()}>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Consumo mensual de inventario — {año}</span>
        </div>
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Mes</th>
                  <th className="text-right">🐔 Gallinas consumidas</th>
                  <th className="text-right">🐹 Cuyes consumidos</th>
                </tr>
              </thead>
              <tbody>
                {meses.map((fila) => {
                  const gallinaVal = parseFloat(fila.gallina ?? 0);
                  const cuyVal = parseFloat(fila.cuy ?? 0);
                  const sinDatos = gallinaVal === 0 && cuyVal === 0;
                  return (
                    <tr key={fila.mes} className={sinDatos ? "text-gray-400" : ""}>
                      <td>{MESES[fila.mes - 1]}</td>
                      <td className="text-right">
                        {sinDatos ? "—" : formatDecimal(gallinaVal)}
                      </td>
                      <td className="text-right">
                        {sinDatos ? "—" : formatDecimal(cuyVal)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr className="font-semibold border-t-2">
                  <td>Total {año}</td>
                  <td className="text-right">{formatDecimal(totales.gallina)}</td>
                  <td className="text-right">{formatDecimal(totales.cuy)}</td>
                </tr>
              </tfoot>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Agregar tab Reportes a ConfiguracionPage.jsx**

El TABS array actual tiene 9 entradas. Agregar la décima:

```js
{ id: "reportes", label: "Reportes" },
```

Agregar el import:
```js
import ReportesTab from "./tabs/ReportesTab";
```

Agregar el render condicional:
```jsx
{tab === "reportes" && <ReportesTab />}
```

- [ ] **Step 4: Correr tests frontend**

```bash
cd Frontend && npx vitest run --reporter=dot 2>&1 | tail -5
```

Esperado: todos en verde.

- [ ] **Step 5: Commit**

```bash
git add Frontend/src/services/inventarioService.js \
        Frontend/src/pages/configuracion/tabs/ReportesTab.jsx \
        Frontend/src/pages/configuracion/ConfiguracionPage.jsx
git commit -m "feat(comparativo): ReportesTab con comparativo mensual de cuyes y gallinas"
```

---

## Verificación final

- [ ] **Backend tests completos**

```bash
cd Backend && mvn test
```

Esperado: todos en verde.

- [ ] **Frontend tests completos**

```bash
cd Frontend && npx vitest run --reporter=verbose 2>&1 | tail -10
```

Esperado: 108+ tests en verde.

- [ ] **Verificación manual Mejora 1 — Denominaciones**

1. Arrancar backend: `cd Backend && mvn spring-boot:run`
2. Abrir frontend: `cd Frontend && npm run dev`
3. Login → ir a `/resumen` → panel de cierre de caja
4. Verificar que aparece la grilla con "Base para mañana" pre-cargada en $0
5. Ingresar: base=350000, billetes100k=2, billetes50k=1, monedasCop=5000
6. Total contado debe mostrar: $705.000
7. Para entregar debe mostrar: $355.000
8. Ejecutar cierre → verificar que se guardó sin error

- [ ] **Verificación manual Mejora 2 — Umbral**

1. Ir a `/configuracion` → tab Inventario
2. Cambiar alerta mínima de gallina a 3 → guardar
3. Ir a Dashboard → el color de gallina cambia según nuevo umbral
4. Con stock de gallina en 0: rojo. Con 2: ámbar. Con 4: verde.

- [ ] **Verificación manual Mejora 3 — Comparativo**

1. Ir a `/configuracion` → tab Reportes
2. Tabla carga con año actual, 12 meses
3. Meses sin datos muestran "—"
4. Cambiar año con flechas → tabla se actualiza
5. Fila de totales al pie suma correctamente

- [ ] **Merge a develop**

```bash
git checkout develop
git merge --no-ff feature/tier2-mejoras-sistema -m "feat: Tier 2 — denominaciones cierre caja, umbral stock configurable, comparativo mensual"
git branch -d feature/tier2-mejoras-sistema
```
