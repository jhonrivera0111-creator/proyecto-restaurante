import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power } from "lucide-react";
import { categoriaService } from "../../../services/productoService";
import ModalCategoria from "../../../components/configuracion/ModalCategoria";

export default function CategoriasTab() {
  const qc = useQueryClient();
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);

  const { data: raw = [], isLoading } = useQuery({
    queryKey: ["categorias"],
    queryFn: categoriaService.listar,
  });

  const categorias = [...raw].sort((a, b) => a.orden - b.orden);

  const mutCreate = useMutation({ mutationFn: categoriaService.crear,                             onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] }) });
  const mutUpdate = useMutation({ mutationFn: ({ id, data }) => categoriaService.actualizar(id, data), onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] }) });
  const mutToggle = useMutation({ mutationFn: categoriaService.toggle,                            onSuccess: () => qc.invalidateQueries({ queryKey: ["categorias"] }) });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nueva categoría
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : categorias.length === 0 ? (
            <div className="empty-state"><p>Sin categorías registradas</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Orden</th><th>Nombre</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {categorias.map((c) => (
                  <tr key={c.id}>
                    <td>{c.orden}</td>
                    <td className="font-medium">{c.nombre}</td>
                    <td>
                      <span className={`badge ${c.activo ? "badge-success" : "badge-danger"}`}>
                        {c.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(c)}>
                          <Pencil size={15} />
                        </button>
                        <button className="btn-icon" title={c.activo ? "Desactivar" : "Activar"} onClick={() => mutToggle.mutate(c.id)}>
                          <Power size={15} className={c.activo ? "text-green-600" : "text-red-600"} />
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
        <ModalCategoria
          categoria={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
