import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Power, Eye } from "lucide-react";
import { empleadoService } from "../../services/empleadoService";
import { formatCOP } from "../../utils/format";
import ModalEmpleado from "../../components/empleados/ModalEmpleado";
import ModalDetalleEmpleado from "../../components/empleados/ModalDetalleEmpleado";

const TIPO_BADGE = {
  QUINCENAL: "badge-info",
  POR_DIA:   "badge-warning",
};

export default function EmpleadosPage() {
  const qc = useQueryClient();
  const [soloActivos, setSoloActivos] = useState(true);
  const [creando, setCreando] = useState(false);
  const [editando, setEditando] = useState(null);
  const [detalle, setDetalle] = useState(null);

  const { data: empleados = [], isLoading } = useQuery({
    queryKey: ["empleados", soloActivos],
    queryFn: () => empleadoService.listar(soloActivos),
  });

  const mutCreate = useMutation({
    mutationFn: empleadoService.crear,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });
  const mutUpdate = useMutation({
    mutationFn: ({ id, data }) => empleadoService.actualizar(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });
  const mutToggle = useMutation({
    mutationFn: empleadoService.toggle,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleados"] }),
  });

  const handleSave = async (data) => {
    if (editando) await mutUpdate.mutateAsync({ id: editando.id, data });
    else await mutCreate.mutateAsync(data);
  };

  return (
    <div className="page-content">
      <div className="page-header">
        <h1 className="page-title">Empleados</h1>
        <div className="flex items-center gap-2">
          <button
            className={`btn btn-sm ${soloActivos ? "btn-primary" : "btn-secondary"}`}
            onClick={() => setSoloActivos((v) => !v)}
          >
            {soloActivos ? "Activos" : "Todos"}
          </button>
          <button className="btn btn-primary" onClick={() => setCreando(true)}>
            <Plus size={15} /> Nuevo empleado
          </button>
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : empleados.length === 0 ? (
            <div className="empty-state"><p>Sin empleados registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th><th>Tipo</th><th>Salario</th><th>Estado</th><th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {empleados.map((emp) => (
                  <tr key={emp.id}>
                    <td className="font-medium">{emp.nombre}</td>
                    <td>
                      <span className={`badge ${TIPO_BADGE[emp.tipo] ?? "badge-gray"}`}>
                        {emp.tipo === "QUINCENAL" ? "Quincenal" : "Por día"}
                      </span>
                    </td>
                    <td>{formatCOP(emp.salario)}</td>
                    <td>
                      <span className={`badge ${emp.activo ? "badge-success" : "badge-danger"}`}>
                        {emp.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button className="btn-icon" title="Editar" onClick={() => setEditando(emp)}>
                          <Pencil size={15} />
                        </button>
                        <button
                          className="btn-icon"
                          title={emp.activo ? "Desactivar" : "Activar"}
                          onClick={() => mutToggle.mutate(emp.id)}
                        >
                          <Power size={15} className={emp.activo ? "text-green-600" : "text-red-600"} />
                        </button>
                        <button className="btn-icon" title="Ver detalle" onClick={() => setDetalle(emp)}>
                          <Eye size={15} />
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
        <ModalEmpleado
          empleado={editando}
          onClose={() => { setCreando(false); setEditando(null); }}
          onSave={handleSave}
        />
      )}
      {detalle && (
        <ModalDetalleEmpleado
          empleado={detalle}
          onClose={() => setDetalle(null)}
        />
      )}
    </div>
  );
}
