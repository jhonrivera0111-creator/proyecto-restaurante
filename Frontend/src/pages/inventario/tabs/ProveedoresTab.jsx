import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, Banknote } from "lucide-react";
import { proveedorService } from "../../../services/proveedorService";
import ModalProveedor from "../../../components/inventario/ModalProveedor";
import ModalAdelantoProveedor from "../../../components/inventario/ModalAdelantoProveedor";

const TIPO_BADGE = {
  CUY:     "badge-success",
  GALLINA: "badge-warning",
  AMBOS:   "badge-info",
  OTROS:   "badge-gray",
};

export default function ProveedoresTab() {
  const qc = useQueryClient();
  const [editando, setEditando] = useState(null);
  const [creando, setCreando] = useState(false);
  const [adelantando, setAdelantando] = useState(null);

  const { data: proveedores = [], isLoading } = useQuery({
    queryKey: ["proveedores"],
    queryFn: () => proveedorService.listar(),
  });

  const mutCreate = useMutation({
    mutationFn: proveedorService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });
  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => proveedorService.actualizar(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });
  const mutToggle = useMutation({
    mutationFn: proveedorService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["proveedores"] }),
  });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div>
      <div className="toolbar">
        <div className="flex-1" />
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nuevo proveedor
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : proveedores.length === 0 ? (
            <div className="empty-state"><p>Sin proveedores registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Nombre</th><th>Tipo</th><th>Teléfono</th><th>Estado</th><th>Adelanto</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {proveedores.map((p) => (
                  <tr key={p.id}>
                    <td className="font-medium">{p.nombre}</td>
                    <td><span className={`badge ${TIPO_BADGE[p.tipo] ?? "badge-gray"}`}>{p.tipo}</span></td>
                    <td>{p.telefono ?? "—"}</td>
                    <td>
                      <span className={`badge ${p.activo ? "badge-success" : "badge-danger"}`}>
                        {p.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-sm btn-secondary"
                        title="Ver/registrar adelantos"
                        onClick={() => setAdelantando(p)}
                      >
                        <Banknote size={13} /> Adelantos
                      </button>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(p)}>
                          <Pencil size={15} />
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
        <ModalProveedor
          proveedor={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}

      {adelantando && (
        <ModalAdelantoProveedor
          proveedor={adelantando}
          onClose={() => setAdelantando(null)}
        />
      )}
    </div>
  );
}
