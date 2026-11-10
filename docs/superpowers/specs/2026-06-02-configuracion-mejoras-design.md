# Diseño: Mejoras módulo Configuración

**Fecha:** 2026-06-02  
**Rama:** develop  
**Alcance:** 4 correcciones/mejoras en las tabs del módulo de Configuración

---

## 1. InventarioTab — Soporte de fracciones en ajuste de stock

### Problema
El input de ajuste de stock usa `step="any"`, lo que hace que las flechas del browser incrementen de 1 en 1. El usuario percibe que solo se aceptan enteros. Los insumos (gallina, cuy) manejan fracciones como 0.5, 0.125.

### Cambios

**`InventarioTab.jsx`**
- `step="any"` → `step="0.001"` para que las flechas incrementen de a 0.001 y el HTML5 valide 3 decimales
- Placeholder: `"Ej: 0.5"` para indicar que se aceptan fracciones
- En el texto del `ConfirmDialog`: usar `formatDecimal(parseFloat(cantidad))` en vez de `parseFloat(cantidad)` crudo para mostrar el valor formateado

### Sin cambios de backend
El endpoint `POST /api/v1/inventario/movimiento` ya acepta `cantidad` como decimal.

---

## 2. TasaUsdTab — Fecha "vigente desde" no se muestra

### Problema
Sin `spring.jackson.serialization.write-dates-as-timestamps=false` en `application.properties`, Jackson puede serializar `LocalDateTime` como array `[2025, 6, 2, 10, 30, 0]`. El frontend hace `new Date([2025,6,2,...])` que produce `Invalid Date`, y `formatFecha` retorna vacío o texto inválido.

### Cambios

**`Backend/src/main/resources/application.properties`**
```
spring.jackson.serialization.write-dates-as-timestamps=false
```
Garantiza que todos los `LocalDateTime` del sistema salgan como ISO string `"2025-06-02T10:30:00"`. Afecta positivamente a todos los endpoints que retornan fechas.

**`TasaUsdTab.jsx`**
- Card "Tasa vigente": reemplazar `formatFecha(vigente.vigenteDesde)` → `formatCreadoEn(vigente.vigenteDesde)`
- Tabla historial: reemplazar `formatFecha(t.vigenteDesde)` → `formatCreadoEn(t.vigenteDesde)`

`formatCreadoEn` muestra "Hoy a las 10:30 AM" o "Lunes 2 de junio · 10:30 AM" — más legible para una fecha de registro reciente que el formato largo de `formatFecha`.

---

## 3. SalonesTab — Eliminar salones (soft delete)

### Problema
No existe endpoint DELETE para salones. La entidad `Pedido` tiene FK `salon_id`, así que la eliminación real rompería la integridad referencial del historial.

### Decisión de diseño
**Soft delete** con campo `eliminado boolean`. El registro queda en BD; los pedidos históricos conservan la referencia con el nombre original. La unicidad de nombre aplica solo entre salones no eliminados. Si se crea un salón con el mismo nombre de uno eliminado, se reactiva el registro existente en vez de crear uno nuevo.

### Cambios backend

**`Salon.java`**
- Agregar: `@Builder.Default private Boolean eliminado = false;`
- Quitar `unique = true` de `@Column(nombre)` — la unicidad se valida en código

**`SalonRepository.java`**
- Agregar: `boolean existsByNombreAndEliminadoFalse(String nombre)`
- Agregar: `boolean existsByNombreAndIdNotAndEliminadoFalse(String nombre, Long id)`
- Agregar: `Optional<Salon> findByNombreAndEliminadoTrue(String nombre)`
- Ajustar los métodos de listado para filtrar `eliminado = false` (o agregar nuevos con ese filtro)

**`ISalonService.java`**
- Agregar método: `void eliminar(Long id)`

**`SalonServiceImpl.java`**
- `eliminar(Long id)`: busca salón, marca `eliminado = true` y `activo = false`, salva, audita con acción `"ELIMINAR"`
- `crear(SalonDTO dto)`: antes de crear, busca `findByNombreAndEliminadoTrue(nombre)`:
  - Si existe → reactivar: `eliminado = false`, `activo = true`, actualizar `aforo`, salvar, auditar con `"REACTIVAR"`
  - Si no existe → flujo normal de creación
- `validarNombre()`: reemplazar `existsByNombre` / `existsByNombreAndIdNot` por las versiones `AndEliminadoFalse`

**`SalonController.java`**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    service.eliminar(id);
    return ResponseEntity.noContent().build();
}
```

### Cambios frontend

**`salonService.js`**
```js
eliminar: (id) => api.delete(`/salones/${id}`).then((r) => r.data),
```

**`SalonesTab.jsx`**
- Importar `Trash2` de lucide-react
- Agregar `mutEliminar` (useMutation → salonService.eliminar, onSuccess invalida `["salones"]`)
- Agregar estado `[eliminandoId, setEliminandoId]`
- En la fila de la tabla: botón `btn-icon` con `Trash2` que abre `ConfirmDialog`:
  - Título: `"Eliminar ${s.nombre}"`
  - Descripción: `"El salón se eliminará. Los pedidos históricos conservarán el salón registrado."`
  - labelOk: `"Sí, eliminar"`
  - onOk: `mutEliminar.mutate(s.id)`

---

## 4. ProductosTab — Refactor (Opción B)

### 4A. Nuevo componente `ModalActualizarPrecio`

**Archivo:** `Frontend/src/components/configuracion/ModalActualizarPrecio.jsx`

Props: `{ producto, onClose, onSave }`

Estructura:
- Modal ancho normal (no `modal-lg`)
- Header: "Actualizar precio"
- Body:
  - Nombre del producto (texto, readonly, contexto visual)
  - Precio actual en gris: "Precio actual: {formatCOP(producto.precio)}"
  - Input "Nuevo precio (COP)" — `type="number"`, `min="0"`, `step="1"`
  - Input "Motivo (opcional)" — texto libre, se envía al backend en el campo `motivo` del `PUT /{id}/precio`
- Footer: Cancelar / Guardar
- Validación: nuevo precio vacío o negativo → error inline

**`ProductosTab.jsx`**
- Eliminar función `handlePrecio` con `window.prompt()`
- Agregar estado `[precioModal, setPrecioModal]` (null o el producto)
- El clic en el precio de la tabla llama `setPrecioModal(p)`
- Renderizar `<ModalActualizarPrecio>` cuando `precioModal !== null`
- La mutation `mutPrecio` pasa a manejar el `{ id, precio, motivo }`

**Backend — extender `PUT /api/v1/productos/{id}/precio` para aceptar motivo:**
El campo `motivo` ya existe en `ProductoPrecioHistorialDTO` y en `registrarPrecio()`, pero está hardcodeado como `"ACTUALIZAR_PRECIO"`. Cambios necesarios:
- `ActualizarPrecioRequest.java`: agregar `String motivo` (opcional, sin `@NotNull`)
- `IProductoService.java`: `actualizarPrecio(Long id, BigDecimal precio, String motivo)`
- `ProductoServiceImpl.actualizarPrecio()`: usar `motivo != null ? motivo : "ACTUALIZAR_PRECIO"` al llamar `registrarPrecio()`
- `ProductoController`: pasar `req.getMotivo()` a `service.actualizarPrecio()`

**Frontend:** la mutation `mutPrecio` envía `{ precio, motivo }` al service.

### 4B. Función `formatFraccion` — composición legible

**Ubicación:** `Frontend/src/utils/pedidoHelpers.js` (o `format.js`)

```js
export const formatFraccion = (cantidad) => {
  if (cantidad === 1)     return "1";
  if (cantidad === 0.5)   return "½";
  if (cantidad === 0.25)  return "¼";
  if (cantidad === 0.125) return "⅛";
  if (cantidad === 0.75)  return "¾";
  return String(cantidad);
};
```

**`ProductosTab.jsx`** — columna Inventario:
```jsx
{p.composicion?.map((c) => `${formatFraccion(c.cantidad)} ${c.insumo}`).join(" + ") || "Sí"}
```

### 4C. `ModalProducto` — sección inventario rediseñada

**`ModalProducto.jsx`**

- Mover el checkbox `afectaInventario` a una fila propia (fila completa, no apilado con Categoría) con label descriptivo: "¿Este producto descuenta inventario al venderse?"

- Cuando `afectaInventario = true`, sección de composición:
  - Header: **"¿Qué se descuenta al vender 1 unidad?"**
  - Descripción pequeña: "Cada cantidad se descuenta proporcional a las unidades vendidas."
  - Filas de insumo: select (Gallina / Cuy) + input cantidad `step="0.001"` + botón quitar (X)
  - Si `composicion.length === 0`: alerta amarilla — "Sin insumos — no se descontará inventario aunque esté marcado"
  - Botón "＋ Agregar insumo" alineado a la derecha del header de la sección

- Cuando `afectaInventario = false`: sección de composición no se renderiza (ya es así, mantener)

---

## Archivos modificados

| Capa | Archivo | Tipo de cambio |
|---|---|---|
| Backend | `application.properties` | Agregar propiedad Jackson |
| Backend | `Salon.java` | Campo `eliminado` |
| Backend | `SalonRepository.java` | 3 métodos nuevos |
| Backend | `ISalonService.java` | Método `eliminar` |
| Backend | `SalonServiceImpl.java` | `eliminar`, `crear`, `validarNombre` |
| Backend | `SalonController.java` | `DELETE /{id}` |
| Frontend | `salonService.js` | Método `eliminar` |
| Frontend | `InventarioTab.jsx` | step + placeholder + formatDecimal |
| Frontend | `TasaUsdTab.jsx` | formatCreadoEn |
| Frontend | `SalonesTab.jsx` | Botón eliminar + ConfirmDialog + mutation |
| Backend | `ActualizarPrecioRequest.java` | Agregar campo `motivo` opcional |
| Backend | `IProductoService.java` | Firma de `actualizarPrecio` con motivo |
| Backend | `ProductoServiceImpl.java` | Propagar motivo a historial |
| Backend | `ProductoController.java` | Pasar `req.getMotivo()` al service |
| Frontend | `ProductosTab.jsx` | ModalActualizarPrecio + formatFraccion |
| Frontend | `ModalProducto.jsx` | Sección inventario rediseñada |
| Frontend | `ModalActualizarPrecio.jsx` | Nuevo componente |
| Frontend | `pedidoHelpers.js` | `formatFraccion` |

---

## Reglas de negocio preservadas

- Los fondos, inventario y auditoría no se ven afectados por estos cambios
- El flujo de pedido (crear → pagar → finalizar) no cambia
- Los snapshots de composición en `PedidoDetalle` no se tocan
- Un salón eliminado no puede ser seleccionado en pedidos nuevos (el endpoint `GET /salones?soloActivos=true` filtra por `activo = false` y `eliminado = false`)

## Definición de terminado

- Backend compila sin errores (`mvn package`)
- Frontend compila sin errores (`npm run build`)
- Las 4 tabs funcionan correctamente: fracciones en inventario, fecha en tasa USD, eliminar/reactivar salones, precio con modal
- No se rompen tabs existentes (Categorías, Usuarios, Fondos)
