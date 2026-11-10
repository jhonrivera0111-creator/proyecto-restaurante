# Configuración — Mejoras módulo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corregir y mejorar 4 tabs del módulo Configuración: fracciones en inventario, fecha vigente en tasa USD, eliminar salones (soft delete con reactivación), y refactor completo de ProductosTab con ModalActualizarPrecio.

**Architecture:** Backend primero (Spring Boot 3.4 / Java 17 / SQLite), luego frontend (React 19 + Vite + Tailwind v4). Los cambios de backend no dependen entre sí salvo el Task 4 (salones) que requiere el Task 3. Los cambios de frontend son todos independientes entre sí.

**Tech Stack:** Java 17, Spring Boot 3.4, MapStruct, Lombok, SQLite; React 19, TanStack Query v5, react-hook-form, Tailwind v4, lucide-react.

---

## File Map

| Archivo | Acción | Propósito |
|---|---|---|
| `Backend/src/main/resources/application.properties` | Modificar | Jackson — no serializar fechas como array |
| `Backend/.../entitys/Salon.java` | Modificar | Campo `eliminado`, quitar `unique` de nombre |
| `Backend/.../repositories/SalonRepository.java` | Modificar | Métodos filtrados por `eliminado` |
| `Backend/.../services/productoService/ISalonService.java` | Modificar | Firma `eliminar(Long id)` |
| `Backend/.../services/productoService/SalonServiceImpl.java` | Modificar | `eliminar`, `crear` (reactivación), `validarNombre` |
| `Backend/.../capaControladores/SalonController.java` | Modificar | `DELETE /{id}` |
| `Backend/.../dto/ActualizarPrecioRequest.java` | Modificar | Agregar `String motivo` opcional |
| `Backend/.../services/productoService/IProductoService.java` | Modificar | Firma `actualizarPrecio` con motivo |
| `Backend/.../services/productoService/ProductoServiceImpl.java` | Modificar | Propagar motivo a historial |
| `Backend/.../capaControladores/ProductoController.java` | Modificar | Pasar `req.getMotivo()` al service |
| `Frontend/src/services/salonService.js` | Modificar | Agregar `eliminar` |
| `Frontend/src/services/productoService.js` | Modificar | `actualizarPrecio` acepta motivo |
| `Frontend/src/pages/configuracion/tabs/InventarioTab.jsx` | Modificar | `step="0.001"`, placeholder, formatDecimal |
| `Frontend/src/pages/configuracion/tabs/TasaUsdTab.jsx` | Modificar | Usar `formatCreadoEn` |
| `Frontend/src/pages/configuracion/tabs/SalonesTab.jsx` | Modificar | Botón eliminar + ConfirmDialog |
| `Frontend/src/utils/pedidoHelpers.js` | Modificar | Agregar `formatFraccion` |
| `Frontend/src/components/configuracion/ModalActualizarPrecio.jsx` | Crear | Modal para cambio de precio |
| `Frontend/src/pages/configuracion/tabs/ProductosTab.jsx` | Modificar | ModalActualizarPrecio + formatFraccion |
| `Frontend/src/components/configuracion/ModalProducto.jsx` | Modificar | Sección inventario rediseñada |

---

## Task 1: Jackson — fechas como ISO string

**Files:**
- Modify: `Backend/src/main/resources/application.properties`

- [ ] **Step 1: Agregar propiedad Jackson**

Abrir `Backend/src/main/resources/application.properties` y agregar al final:

```properties
spring.jackson.serialization.write-dates-as-timestamps=false
```

El archivo completo queda:

```properties
app.datasource.path=./DataBase/casaverde.db
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
app.backup.dir=./Backups
app.admin.default-password=admin123
spring.jpa.database-platform=com.casaverde.backend.config.dialect.SQLiteDialect
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=update
spring.jpa.defer-datasource-initialization=true
spring.jpa.open-in-view=false
server.port=5020
spring.jackson.serialization.write-dates-as-timestamps=false
```

- [ ] **Step 2: Verificar compilación**

```bash
cd Backend && mvn package -DskipTests -q
```

Esperado: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/resources/application.properties
git commit -m "config: jackson serializa LocalDateTime como ISO string"
```

---

## Task 2: Salon — campo eliminado + quitar unique de nombre

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Salon.java`

- [ ] **Step 1: Modificar entidad**

Reemplazar el contenido completo de `Salon.java`:

```java
package com.casaverde.backend.capaAccesoADatos.models.entitys;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private Integer aforo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminado = false;
}
```

> Nota: se quitó `unique = true` de `@Column(nombre)`. Hibernate con `ddl-auto=update` no elimina constraints existentes automáticamente en SQLite, pero la unicidad ya no se validará por JPA — la validación queda en el servicio.

- [ ] **Step 2: Verificar compilación**

```bash
cd Backend && mvn package -DskipTests -q
```

Esperado: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/models/entitys/Salon.java
git commit -m "feat(salones): campo eliminado para soft delete"
```

---

## Task 3: SalonRepository — métodos filtrados por eliminado

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/SalonRepository.java`

- [ ] **Step 1: Reemplazar el contenido completo del repositorio**

```java
package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByEliminadoFalseOrderByNombreAsc();

    List<Salon> findByActivoTrueAndEliminadoFalseOrderByNombreAsc();

    boolean existsByNombreAndEliminadoFalse(String nombre);

    boolean existsByNombreAndIdNotAndEliminadoFalse(String nombre, Long id);

    Optional<Salon> findByNombreAndEliminadoTrue(String nombre);
}
```

> Los métodos anteriores (`findAllByOrderByNombreAsc`, `findByActivoTrueOrderByNombreAsc`, `existsByNombre`, `existsByNombreAndIdNot`) se reemplazan. Se actualizarán las referencias en el siguiente task.

- [ ] **Step 2: Verificar compilación** (fallará hasta que SalonServiceImpl se actualice)

```bash
cd Backend && mvn package -DskipTests -q 2>&1 | head -30
```

Esperado: errores en `SalonServiceImpl` referenciando métodos eliminados — confirma que los cambios son necesarios. Avanzar al Task 4.

---

## Task 4: ISalonService + SalonServiceImpl + SalonController — soft delete

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/ISalonService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/SalonServiceImpl.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaControladores/SalonController.java`

- [ ] **Step 1: Agregar `eliminar` a la interfaz**

Abrir `ISalonService.java` y agregar el método `eliminar`. El archivo completo debe contener:

```java
package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import com.casaverde.backend.fachadaServices.dto.SalonDTO;

public interface ISalonService {
    List<SalonDTO> listarTodos();
    List<SalonDTO> listarActivos();
    SalonDTO buscarPorId(Long id);
    SalonDTO crear(SalonDTO dto);
    SalonDTO actualizar(Long id, SalonDTO dto);
    SalonDTO toggleActivo(Long id);
    void eliminar(Long id);
}
```

- [ ] **Step 2: Reemplazar SalonServiceImpl completo**

```java
package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;
import com.casaverde.backend.fachadaServices.dto.SalonDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.SalonMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalonServiceImpl implements ISalonService {

    private final SalonRepository repo;
    private final IAuditoriaService auditoriaService;
    private final SalonMapper salonMapper;

    @Transactional(readOnly = true)
    public List<SalonDTO> listarTodos() {
        return repo.findByEliminadoFalseOrderByNombreAsc().stream()
                .map(salonMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalonDTO> listarActivos() {
        return repo.findByActivoTrueAndEliminadoFalseOrderByNombreAsc().stream()
                .map(salonMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalonDTO buscarPorId(Long id) {
        return salonMapper.toDTO(findOrThrow(id));
    }

    @Transactional
    public SalonDTO crear(SalonDTO dto) {
        String nombre = dto.getNombre() != null ? dto.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del salón es obligatorio");
        }
        Optional<Salon> eliminado = repo.findByNombreAndEliminadoTrue(nombre);
        if (eliminado.isPresent()) {
            Salon salon = eliminado.get();
            salon.setEliminado(false);
            salon.setActivo(true);
            salon.setAforo(dto.getAforo());
            Salon guardado = repo.save(salon);
            auditoriaService.registrar("salon", guardado.getId(), "REACTIVAR",
                    "Salón reactivado: " + guardado.getNombre());
            return salonMapper.toDTO(guardado);
        }
        validarNombre(nombre, null);
        Salon salon = Salon.builder()
                .nombre(nombre)
                .aforo(dto.getAforo())
                .activo(true)
                .build();
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CREAR",
                "Salón creado: " + guardado.getNombre());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public SalonDTO actualizar(Long id, SalonDTO dto) {
        Salon salon = findOrThrow(id);
        validarNombre(dto.getNombre(), id);
        salon.setNombre(dto.getNombre().trim());
        salon.setAforo(dto.getAforo());
        if (dto.getActivo() != null)
            salon.setActivo(dto.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "ACTUALIZAR",
                "Salón actualizado: " + guardado.getNombre());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public SalonDTO toggleActivo(Long id) {
        Salon salon = findOrThrow(id);
        salon.setActivo(!salon.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CAMBIAR_ESTADO",
                "Salón activo=" + guardado.getActivo());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Salon salon = findOrThrow(id);
        salon.setEliminado(true);
        salon.setActivo(false);
        repo.save(salon);
        auditoriaService.registrar("salon", salon.getId(), "ELIMINAR",
                "Salón eliminado: " + salon.getNombre());
    }

    private void validarNombre(String nombre, Long idExcluir) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidacionException("El nombre del salón es obligatorio");
        }
        boolean existe = idExcluir == null
                ? repo.existsByNombreAndEliminadoFalse(nombre.trim())
                : repo.existsByNombreAndIdNotAndEliminadoFalse(nombre.trim(), idExcluir);
        if (existe) {
            throw new ValidacionException("Ya existe un salón con el nombre: " + nombre.trim());
        }
    }

    private Salon findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + id));
    }
}
```

- [ ] **Step 3: Agregar DELETE al controller**

Reemplazar el contenido completo de `SalonController.java`:

```java
package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.SalonDTO;
import com.casaverde.backend.fachadaServices.services.productoService.ISalonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/salones")
@RequiredArgsConstructor
public class SalonController {

    private final ISalonService service;

    @GetMapping
    public List<SalonDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos ? service.listarActivos() : service.listarTodos();
    }

    @GetMapping("/{id}")
    public SalonDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<SalonDTO> crear(@RequestBody SalonDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public SalonDTO actualizar(@PathVariable Long id, @RequestBody SalonDTO dto) {
        return service.actualizar(id, dto);
    }

    @PutMapping("/{id}/toggle")
    public SalonDTO toggleActivo(@PathVariable Long id) {
        return service.toggleActivo(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Verificar compilación**

```bash
cd Backend && mvn package -DskipTests -q
```

Esperado: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/capaAccesoADatos/repositories/SalonRepository.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/ISalonService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/SalonServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/capaControladores/SalonController.java
git commit -m "feat(salones): soft delete con reactivación y endpoint DELETE"
```

---

## Task 5: Precio con motivo — backend

**Files:**
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ActualizarPrecioRequest.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IProductoService.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/ProductoServiceImpl.java`
- Modify: `Backend/src/main/java/com/casaverde/backend/capaControladores/ProductoController.java`

- [ ] **Step 1: Agregar `motivo` a ActualizarPrecioRequest**

```java
package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPrecioRequest {
    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precio;

    private String motivo;
}
```

- [ ] **Step 2: Actualizar firma en IProductoService**

Encontrar la línea:
```java
ProductoDTO actualizarPrecio(Long id, BigDecimal precio);
```
Reemplazar por:
```java
ProductoDTO actualizarPrecio(Long id, BigDecimal precio, String motivo);
```

- [ ] **Step 3: Actualizar ProductoServiceImpl.actualizarPrecio**

Encontrar el método:
```java
public ProductoDTO actualizarPrecio(Long id, BigDecimal precio) {
    precio = monetaryService.normalizarPrecioCop(precio);
    Producto producto = findOrThrow(id);
    BigDecimal precioAnterior = producto.getPrecio();
    if (precioAnterior.compareTo(precio) == 0) {
        return productoMapper.toDTO(producto);
    }
    producto.setPrecio(precio);
    Producto guardado = productoRepo.save(producto);
    registrarPrecio(guardado, precioAnterior, precio, "ACTUALIZAR_PRECIO");
    int afectados = propagarPrecioAPedidosAbiertos(id, precio);
    auditoriaService.registrar("producto", guardado.getId(), "ACTUALIZAR_PRECIO",
            "Precio actualizado de COP " + precioAnterior + " a COP " + precio
                    + (afectados > 0 ? "; " + afectados + " detalle(s) de pedidos abiertos actualizados" : ""));
    return productoMapper.toDTO(guardado);
}
```

Reemplazar por:
```java
public ProductoDTO actualizarPrecio(Long id, BigDecimal precio, String motivo) {
    precio = monetaryService.normalizarPrecioCop(precio);
    Producto producto = findOrThrow(id);
    BigDecimal precioAnterior = producto.getPrecio();
    if (precioAnterior.compareTo(precio) == 0) {
        return productoMapper.toDTO(producto);
    }
    producto.setPrecio(precio);
    Producto guardado = productoRepo.save(producto);
    String motivoFinal = (motivo != null && !motivo.isBlank()) ? motivo : "ACTUALIZAR_PRECIO";
    registrarPrecio(guardado, precioAnterior, precio, motivoFinal);
    int afectados = propagarPrecioAPedidosAbiertos(id, precio);
    auditoriaService.registrar("producto", guardado.getId(), "ACTUALIZAR_PRECIO",
            "Precio actualizado de COP " + precioAnterior + " a COP " + precio
                    + (afectados > 0 ? "; " + afectados + " detalle(s) de pedidos abiertos actualizados" : ""));
    return productoMapper.toDTO(guardado);
}
```

- [ ] **Step 4: Actualizar ProductoController**

Encontrar:
```java
return service.actualizarPrecio(id, req.getPrecio());
```
Reemplazar por:
```java
return service.actualizarPrecio(id, req.getPrecio(), req.getMotivo());
```

- [ ] **Step 5: Verificar compilación**

```bash
cd Backend && mvn package -DskipTests -q
```

Esperado: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add Backend/src/main/java/com/casaverde/backend/fachadaServices/dto/ActualizarPrecioRequest.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/IProductoService.java \
        Backend/src/main/java/com/casaverde/backend/fachadaServices/services/productoService/ProductoServiceImpl.java \
        Backend/src/main/java/com/casaverde/backend/capaControladores/ProductoController.java
git commit -m "feat(productos): propagar motivo al historial de precios"
```

---

## Task 6: InventarioTab — fracciones

**Files:**
- Modify: `Frontend/src/pages/configuracion/tabs/InventarioTab.jsx`

- [ ] **Step 1: Ajustar input y ConfirmDialog**

En la función `InsumoRow`, hacer tres cambios:

**Cambio 1** — input step y placeholder (líneas ~93-94):
```jsx
// Antes:
type="number"
step="any"
min="0"
placeholder="Nuevo stock"

// Después:
type="number"
step="0.001"
min="0"
placeholder="Ej: 0.5"
```

**Cambio 2** — descripción del ConfirmDialog (línea ~119):
```jsx
// Antes:
descripcion={`El stock pasará de ${formatDecimal(insumo.stockActual)} a ${parseFloat(cantidad)}. Este cambio queda registrado en el historial de inventario.`}

// Después:
descripcion={`El stock pasará de ${formatDecimal(insumo.stockActual)} a ${formatDecimal(parseFloat(cantidad))}. Este cambio queda registrado en el historial de inventario.`}
```

`formatDecimal` ya está importado en la línea 4 del archivo — no necesita nuevo import.

- [ ] **Step 2: Verificar build frontend**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

Esperado: sin errores de compilación.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/InventarioTab.jsx
git commit -m "fix(inventario): step 0.001 y formatDecimal en ajuste de stock"
```

---

## Task 7: TasaUsdTab — fecha vigente desde

**Files:**
- Modify: `Frontend/src/pages/configuracion/tabs/TasaUsdTab.jsx`

- [ ] **Step 1: Cambiar import y reemplazar formatFecha por formatCreadoEn**

**Cambio 1** — línea 5, import:
```jsx
// Antes:
import { formatCOP, formatFecha } from "../../../utils/format";

// Después:
import { formatCOP, formatCreadoEn } from "../../../utils/format";
```

**Cambio 2** — en la card de tasa vigente (~línea 68):
```jsx
// Antes:
Vigente desde: {formatFecha(vigente.vigenteDesde)}

// Después:
Vigente desde: {formatCreadoEn(vigente.vigenteDesde)}
```

**Cambio 3** — en la tabla de historial (~línea 110):
```jsx
// Antes:
<td className="text-gray-500">{formatFecha(t.vigenteDesde)}</td>

// Después:
<td className="text-gray-500">{formatCreadoEn(t.vigenteDesde)}</td>
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

Esperado: sin errores.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/TasaUsdTab.jsx
git commit -m "fix(tasa-usd): mostrar fecha vigente con formatCreadoEn"
```

---

## Task 8: salonService + SalonesTab — botón eliminar

**Files:**
- Modify: `Frontend/src/services/salonService.js`
- Modify: `Frontend/src/pages/configuracion/tabs/SalonesTab.jsx`

- [ ] **Step 1: Agregar método eliminar al servicio**

Reemplazar `salonService.js` completo:

```js
import api from "./api";

export const salonService = {
  listar: () => api.get("/salones").then((r) => r.data),
  crear: (data) => api.post("/salones", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/salones/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/salones/${id}/toggle`).then((r) => r.data),
  eliminar: (id) => api.delete(`/salones/${id}`).then((r) => r.data),
};
```

- [ ] **Step 2: Agregar botón eliminar en SalonesTab**

Reemplazar `SalonesTab.jsx` completo:

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, Trash2 } from "lucide-react";
import { salonService } from "../../../services/salonService";
import ModalSalon from "../../../components/configuracion/ModalSalon";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";

export default function SalonesTab() {
  const qc = useQueryClient();
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [eliminandoId, setEliminandoId] = useState(null);

  const { data: salones = [], isLoading } = useQuery({
    queryKey: ["salones"],
    queryFn: salonService.listar,
  });

  const mutCreate   = useMutation({ mutationFn: salonService.crear,    onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });
  const mutUpdate   = useMutation({ mutationFn: ({ id, data }) => salonService.actualizar(id, data), onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });
  const mutToggle   = useMutation({ mutationFn: salonService.toggle,   onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });
  const mutEliminar = useMutation({ mutationFn: salonService.eliminar, onSuccess: () => qc.invalidateQueries({ queryKey: ["salones"] }) });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  const salonEliminando = salones.find((s) => s.id === eliminandoId);

  return (
    <div>
      <div className="toolbar">
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo salón
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : salones.length === 0 ? (
            <div className="empty-state"><p>Sin salones registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Nombre</th><th>Aforo</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {salones.map((s) => (
                  <tr key={s.id}>
                    <td className="font-medium">{s.nombre}</td>
                    <td>{s.aforo ?? "—"}</td>
                    <td>
                      <span className={`badge ${s.activo ? "badge-success" : "badge-danger"}`}>
                        {s.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(s)}>
                          <Pencil size={15} />
                        </button>
                        <button className="btn-icon" title={s.activo ? "Desactivar" : "Activar"} onClick={() => mutToggle.mutate(s.id)}>
                          <Power size={15} className={s.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                        <button className="btn-icon" title="Eliminar" onClick={() => setEliminandoId(s.id)}>
                          <Trash2 size={15} className="text-red-500" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(creando || editando) && (
        <ModalSalon
          salon={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}

      {eliminandoId && salonEliminando && (
        <ConfirmDialog
          emoji="🗑️"
          titulo={`Eliminar "${salonEliminando.nombre}"`}
          descripcion="El salón se eliminará. Los pedidos históricos conservarán el salón registrado."
          labelOk="Sí, eliminar"
          onOk={() => { mutEliminar.mutate(eliminandoId); setEliminandoId(null); }}
          onCancelar={() => setEliminandoId(null)}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 3: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

Esperado: sin errores.

- [ ] **Step 4: Commit**

```bash
git add Frontend/src/services/salonService.js \
        Frontend/src/pages/configuracion/tabs/SalonesTab.jsx
git commit -m "feat(salones): botón eliminar con confirmación y soft delete"
```

---

## Task 9: pedidoHelpers.js — formatFraccion

**Files:**
- Modify: `Frontend/src/utils/pedidoHelpers.js`

- [ ] **Step 1: Agregar función al final del archivo**

Abrir `Frontend/src/utils/pedidoHelpers.js` y agregar al final:

```js
export const formatFraccion = (cantidad) => {
  if (cantidad === 1)      return "1";
  if (cantidad === 0.5)    return "½";
  if (cantidad === 0.25)   return "¼";
  if (cantidad === 0.125)  return "⅛";
  if (cantidad === 0.75)   return "¾";
  if (cantidad === 0.375)  return "⅜";
  if (cantidad === 0.625)  return "⅝";
  if (cantidad === 0.875)  return "⅞";
  return String(cantidad);
};
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

Esperado: sin errores.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/utils/pedidoHelpers.js
git commit -m "feat(utils): formatFraccion para cantidades de inventario"
```

---

## Task 10: productoService.js — actualizarPrecio con motivo

**Files:**
- Modify: `Frontend/src/services/productoService.js`

- [ ] **Step 1: Actualizar método actualizarPrecio**

Reemplazar el contenido completo:

```js
import api from "./api";

export const productoService = {
  listar: (soloActivos = false) =>
    api.get("/productos", { params: { soloActivos } }).then((r) => r.data),
  historialPrecios: (id) => api.get(`/productos/${id}/precios`).then((r) => r.data),
  crear: (data) => api.post("/productos", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/productos/${id}`, data).then((r) => r.data),
  actualizarPrecio: (id, precio, motivo) =>
    api.put(`/productos/${id}/precio`, { precio, motivo }).then((r) => r.data),
  toggle: (id) => api.put(`/productos/${id}/toggle`).then((r) => r.data),
};

export const categoriaService = {
  listar: (soloActivas = false) =>
    api.get("/categorias", { params: { soloActivas } }).then((r) => r.data),
  crear: (data) => api.post("/categorias", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/categorias/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/categorias/${id}/toggle`).then((r) => r.data),
};
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/services/productoService.js
git commit -m "feat(productos): actualizarPrecio envía motivo al backend"
```

---

## Task 11: ModalActualizarPrecio — nuevo componente

**Files:**
- Create: `Frontend/src/components/configuracion/ModalActualizarPrecio.jsx`

- [ ] **Step 1: Crear el archivo**

```jsx
import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { formatCOP } from "../../utils/format";

export default function ModalActualizarPrecio({ producto, onClose, onSave }) {
  const [precio, setPrecio] = useState("");
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    const val = parseFloat(precio);
    if (!precio || isNaN(val) || val < 0) {
      setError("Ingrese un precio válido (0 o mayor)");
      return;
    }
    setSaving(true);
    try {
      await onSave({ id: producto.id, precio: val, motivo: motivo.trim() || null });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Actualizar precio</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <p className="font-semibold text-verde-oscuro mb-1">{producto.nombre}</p>
            <p className="text-sm text-gray-500 mb-4">
              Precio actual:{" "}
              <span className="font-semibold text-gray-700">{formatCOP(producto.precio)}</span>
            </p>
            <div className="form-group">
              <label className="form-label">Nuevo precio (COP) *</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="1"
                placeholder="Ej: 25000"
                value={precio}
                onChange={(e) => { setPrecio(e.target.value); setError(""); }}
                autoFocus
              />
            </div>
            <div className="form-group mb-0">
              <label className="form-label">Motivo (opcional)</label>
              <input
                className="form-control"
                type="text"
                placeholder="Ej: Ajuste inflación"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar precio"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/components/configuracion/ModalActualizarPrecio.jsx
git commit -m "feat(productos): ModalActualizarPrecio con motivo"
```

---

## Task 12: ProductosTab — refactor completo

**Files:**
- Modify: `Frontend/src/pages/configuracion/tabs/ProductosTab.jsx`

- [ ] **Step 1: Reemplazar ProductosTab completo**

```jsx
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { History, Plus, Pencil, Power, Search } from "lucide-react";
import { productoService, categoriaService } from "../../../services/productoService";
import { formatCOP } from "../../../utils/format";
import { formatFraccion } from "../../../utils/pedidoHelpers";
import ModalProducto from "../../../components/configuracion/ModalProducto";
import ModalHistorialPrecios from "../../../components/configuracion/ModalHistorialPrecios";
import ModalActualizarPrecio from "../../../components/configuracion/ModalActualizarPrecio";

export default function ProductosTab() {
  const qc = useQueryClient();
  const [busqueda, setBusqueda] = useState("");
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [historialDe, setHistorialDe] = useState(null);
  const [precioModal, setPrecioModal] = useState(null);

  const { data: productos = [], isLoading } = useQuery({
    queryKey: ["productos", soloActivos],
    queryFn: () => productoService.listar(soloActivos),
  });

  const { data: categorias = [] } = useQuery({
    queryKey: ["categorias"],
    queryFn: () => categoriaService.listar(),
  });

  const { data: historialPrecios = [], isLoading: loadingHistorial } = useQuery({
    queryKey: ["producto-precios", historialDe?.id],
    queryFn: () => productoService.historialPrecios(historialDe.id),
    enabled: !!historialDe,
  });

  const mutCreate = useMutation({
    mutationFn: productoService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => productoService.actualizar(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["pedidos"] });
    },
  });

  const mutToggle = useMutation({
    mutationFn: productoService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const mutPrecio = useMutation({
    mutationFn: ({ id, precio, motivo }) => productoService.actualizarPrecio(id, precio, motivo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["pedidos"] });
    },
  });

  const filtrados = productos.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="search-wrap">
          <Search size={16} />
          <input
            className="form-control"
            placeholder="Buscar producto…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
        <label className="flex items-center gap-1.5 text-[13px] cursor-pointer">
          <input type="checkbox" checked={soloActivos} onChange={(e) => setSoloActivos(e.target.checked)} />
          Solo activos
        </label>
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo producto
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : filtrados.length === 0 ? (
            <div className="empty-state"><p>Sin productos</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Categoría</th>
                  <th>Precio</th>
                  <th>Inventario</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtrados.map((p) => (
                  <tr key={p.id}>
                    <td className="font-medium">{p.nombre}</td>
                    <td className="text-gray-500">{p.categoriaNombre ?? "—"}</td>
                    <td>
                      <span
                        className="cursor-pointer underline decoration-dotted"
                        onClick={() => setPrecioModal(p)}
                        title="Clic para cambiar precio"
                      >
                        {formatCOP(p.precio)}
                      </span>
                    </td>
                    <td>
                      {p.afectaInventario ? (
                        <span className="badge badge-info">
                          {p.composicion?.length > 0
                            ? p.composicion.map((c) => `${formatFraccion(c.cantidad)} ${c.insumo}`).join(" + ")
                            : "Sí"}
                        </span>
                      ) : (
                        <span className="badge badge-gray">No</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${p.activo ? "badge-success" : "badge-danger"}`}>
                        {p.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(p)}>
                          <Pencil size={15} />
                        </button>
                        <button className="btn-icon" title="Historial de precios" onClick={() => setHistorialDe(p)}>
                          <History size={15} />
                        </button>
                        <button
                          className="btn-icon"
                          title={p.activo ? "Desactivar" : "Activar"}
                          onClick={() => mutToggle.mutate(p.id)}
                        >
                          <Power size={15} className={p.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(creando || editando) && (
        <ModalProducto
          producto={editando}
          categorias={categorias}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}

      {historialDe && (
        <ModalHistorialPrecios
          producto={historialDe}
          historial={historialPrecios}
          loading={loadingHistorial}
          onClose={() => setHistorialDe(null)}
        />
      )}

      {precioModal && (
        <ModalActualizarPrecio
          producto={precioModal}
          onClose={() => setPrecioModal(null)}
          onSave={(data) => mutPrecio.mutateAsync(data)}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/pages/configuracion/tabs/ProductosTab.jsx
git commit -m "feat(productos): ModalActualizarPrecio, formatFraccion en tabla"
```

---

## Task 13: ModalProducto — sección inventario rediseñada

**Files:**
- Modify: `Frontend/src/components/configuracion/ModalProducto.jsx`

- [ ] **Step 1: Reemplazar ModalProducto completo**

```jsx
import { useState } from "react";
import { Plus, X, AlertTriangle } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalProducto({ producto, categorias, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre:           producto?.nombre           ?? "",
    precio:           producto?.precio           ?? 0,
    categoriaId:      producto?.categoriaId      ?? "",
    afectaInventario: producto?.afectaInventario ?? false,
    composicion:      producto?.composicion      ?? [],
  });
  const [error, setError]   = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const agregarComp = () =>
    setForm((f) => ({ ...f, composicion: [...f.composicion, { insumo: "gallina", cantidad: 1 }] }));

  const quitarComp = (i) =>
    setForm((f) => ({ ...f, composicion: f.composicion.filter((_, idx) => idx !== i) }));

  const setComp = (i, k, v) =>
    setForm((f) => {
      const comp = [...f.composicion];
      comp[i] = { ...comp[i], [k]: v };
      return { ...f, composicion: comp };
    });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({
        ...form,
        precio:      parseFloat(form.precio) || 0,
        categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
        composicion: form.afectaInventario
          ? form.composicion.map((c) => ({ ...c, cantidad: parseFloat(c.cantidad) || 0 }))
          : [],
      });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">{producto ? "Editar producto" : "Nuevo producto"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Nombre *</label>
                <input
                  className="form-control"
                  value={form.nombre}
                  onChange={(e) => set("nombre", e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Precio (COP)</label>
                <input
                  className="form-control"
                  type="number"
                  min="0"
                  step="1"
                  value={form.precio}
                  onChange={(e) => set("precio", e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Categoría</label>
              <select
                className="form-control"
                value={form.categoriaId}
                onChange={(e) => set("categoriaId", e.target.value)}
              >
                <option value="">— Sin categoría —</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombre}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <div className="flex items-center gap-2.5">
                <input
                  type="checkbox"
                  id="afecta"
                  checked={form.afectaInventario}
                  onChange={(e) => set("afectaInventario", e.target.checked)}
                />
                <label htmlFor="afecta" className="cursor-pointer font-medium">
                  ¿Este producto descuenta inventario al venderse?
                </label>
              </div>
            </div>

            {form.afectaInventario && (
              <div className="border border-gray-200 rounded-lg p-3 bg-gray-50">
                <div className="flex items-center justify-between mb-1">
                  <div>
                    <p className="font-semibold text-[13px] text-verde-oscuro">
                      ¿Qué se descuenta al vender 1 unidad?
                    </p>
                    <p className="text-[12px] text-gray-500">
                      Cada cantidad se descuenta proporcional a las unidades vendidas.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm shrink-0"
                    onClick={agregarComp}
                  >
                    <Plus size={13} /> Agregar insumo
                  </button>
                </div>

                {form.composicion.length === 0 ? (
                  <div className="flex items-center gap-2 mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-[12px] text-yellow-800">
                    <AlertTriangle size={14} className="shrink-0" />
                    Sin insumos — no se descontará inventario aunque esté marcado
                  </div>
                ) : (
                  <div className="flex flex-col gap-1.5 mt-2">
                    {form.composicion.map((c, i) => (
                      <div key={i} className="flex items-center gap-2">
                        <select
                          className="form-control flex-1"
                          value={c.insumo}
                          onChange={(e) => setComp(i, "insumo", e.target.value)}
                        >
                          <option value="gallina">Gallina</option>
                          <option value="cuy">Cuy</option>
                        </select>
                        <input
                          className="form-control w-[110px]"
                          type="number"
                          step="0.001"
                          min="0"
                          placeholder="Cantidad"
                          value={c.cantidad}
                          onChange={(e) => setComp(i, "cantidad", e.target.value)}
                        />
                        <button
                          type="button"
                          className="btn-icon shrink-0"
                          onClick={() => quitarComp(i)}
                        >
                          <X size={15} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verificar build**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

Esperado: sin errores.

- [ ] **Step 3: Commit**

```bash
git add Frontend/src/components/configuracion/ModalProducto.jsx
git commit -m "feat(productos): sección inventario más clara en modal de producto"
```

---

## Verificación final

- [ ] **Verificar tests backend existentes**

```bash
cd Backend && mvn test 2>&1 | tail -20
```

Esperado: todos los tests pasan (o ningún test si no hay escritos).

- [ ] **Verificar build frontend completo**

```bash
cd Frontend && npm run build 2>&1 | tail -10
```

- [ ] **Probar las 4 tabs manualmente**

1. **Inventario:** Intentar ajustar stock a `0.5` — debe aceptarlo. El confirm debe mostrar "0.5" formateado.
2. **Tasa USD:** La card de tasa vigente debe mostrar "Vigente desde: Hoy a las..." o "Lunes 2 de junio · 10:30 AM".
3. **Salones:** Crear → Eliminar → Crear con mismo nombre (debe reactivar, no crear duplicado).
4. **Productos:** Clic en precio → ModalActualizarPrecio. Badge de inventario muestra "½ gallina". Modal nuevo producto tiene sección inventario clara con aviso amarillo si está vacía.
