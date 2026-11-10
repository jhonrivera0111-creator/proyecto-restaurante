import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { History, Plus, Pencil, Power, Search, Trash2, GripVertical } from "lucide-react";
import { DndContext, closestCenter, PointerSensor, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, verticalListSortingStrategy, useSortable, arrayMove } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { productoService, categoriaService } from "../../../services/productoService";
import { formatCOP } from "../../../utils/format";
import { formatFraccion } from "../../../utils/pedidoHelpers";
import ModalProducto from "../../../components/configuracion/ModalProducto";
import ModalHistorialPrecios from "../../../components/configuracion/ModalHistorialPrecios";
import ModalActualizarPrecio from "../../../components/configuracion/ModalActualizarPrecio";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";

/** Fila arrastrable de la tabla de productos. El agarre (GripVertical) es el
 * único punto de arrastre — el resto de la fila conserva sus clics normales
 * (editar precio, toggle, etc). */
function FilaProducto({ producto: p, dragDisabled, onEditar, onHistorial, onPrecio, onToggle, onEliminar }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: p.id,
    disabled: dragDisabled,
  });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <tr ref={setNodeRef} style={style}>
      <td className="w-8 px-2">
        {!dragDisabled && (
          <span
            {...attributes}
            {...listeners}
            className="cursor-grab active:cursor-grabbing text-gray-300 hover:text-gray-500 inline-flex touch-none"
            title="Arrastrar para reordenar"
          >
            <GripVertical size={15} />
          </span>
        )}
      </td>
      <td className="font-medium">{p.nombre}</td>
      <td className="text-gray-500">{p.categoriaNombre ?? "—"}</td>
      <td>
        <span
          className="cursor-pointer underline decoration-dotted"
          onClick={() => onPrecio(p)}
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
          <button className="btn-icon" title="Editar" onClick={() => onEditar(p)}>
            <Pencil size={15} />
          </button>
          <button className="btn-icon" title="Historial de precios" onClick={() => onHistorial(p)}>
            <History size={15} />
          </button>
          <button
            className="btn-icon"
            title={p.activo ? "Desactivar" : "Activar"}
            onClick={() => onToggle(p.id)}
          >
            <Power size={15} className={p.activo ? "text-green-600" : "text-red-600"} />
          </button>
          <button
            className="btn-icon text-red-500"
            title="Eliminar producto"
            onClick={() => onEliminar(p.id)}
          >
            <Trash2 size={15} />
          </button>
        </div>
      </td>
    </tr>
  );
}

export default function ProductosTab() {
  const qc = useQueryClient();
  const [busqueda, setBusqueda] = useState("");
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [historialDe, setHistorialDe] = useState(null);
  const [precioModal, setPrecioModal] = useState(null);
  const [eliminandoId, setEliminandoId] = useState(null);

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

  const mutEliminar = useMutation({
    mutationFn: productoService.eliminar,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      setEliminandoId(null);
    },
  });

  const mutPrecio = useMutation({
    mutationFn: ({ id, precio, motivo }) => productoService.actualizarPrecio(id, precio, motivo),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["pedidos"] });
    },
  });

  const mutReordenar = useMutation({
    mutationFn: productoService.reordenar,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["productos"] });
      qc.invalidateQueries({ queryKey: ["productos-activos"] });
    },
    // Si falla, descarta el reorden optimista y trae el orden real del servidor.
    onError: () => qc.invalidateQueries({ queryKey: ["productos"] }),
  });

  const filtrados = productos.filter((p) =>
    p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );
  // Arrastrar solo tiene sentido sobre la lista completa — con un filtro de
  // búsqueda activo, reordenar el subconjunto visible dejaría huecos/orden
  // ambiguo respecto a los productos ocultos.
  const dragDisabled = busqueda.trim() !== "";

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const handleDragEnd = (event) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = filtrados.findIndex((p) => p.id === active.id);
    const newIndex = filtrados.findIndex((p) => p.id === over.id);
    if (oldIndex === -1 || newIndex === -1) return;
    const nuevoOrden = arrayMove(filtrados, oldIndex, newIndex);
    qc.setQueryData(["productos", soloActivos], nuevoOrden);
    mutReordenar.mutate(nuevoOrden.map((p) => p.id));
  };

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
        {dragDisabled && (
          <span className="text-[12px] text-gray-400">Limpia el buscador para reordenar arrastrando</span>
        )}
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
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
              <table>
                <thead>
                  <tr>
                    <th></th>
                    <th>Nombre</th>
                    <th>Categoría</th>
                    <th>Precio</th>
                    <th>Inventario</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  <SortableContext items={filtrados.map((p) => p.id)} strategy={verticalListSortingStrategy}>
                    {filtrados.map((p) => (
                      <FilaProducto
                        key={p.id}
                        producto={p}
                        dragDisabled={dragDisabled}
                        onEditar={setEditando}
                        onHistorial={setHistorialDe}
                        onPrecio={setPrecioModal}
                        onToggle={(id) => mutToggle.mutate(id)}
                        onEliminar={setEliminandoId}
                      />
                    ))}
                  </SortableContext>
                </tbody>
              </table>
            </DndContext>
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

      {eliminandoId !== null && (() => {
        const p = productos.find((x) => x.id === eliminandoId);
        return p ? (
          <ConfirmDialog
            emoji="🗑️"
            titulo={`Eliminar ${p.nombre}`}
            descripcion="El producto se eliminará permanentemente. Los pedidos históricos conservan el nombre y precio registrados al momento de la venta."
            labelOk="Sí, eliminar"
            onOk={() => mutEliminar.mutate(eliminandoId)}
            onCancelar={() => setEliminandoId(null)}
          />
        ) : null;
      })()}
    </div>
  );
}
