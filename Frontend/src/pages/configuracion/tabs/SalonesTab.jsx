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
