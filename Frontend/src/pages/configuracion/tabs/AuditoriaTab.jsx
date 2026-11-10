import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { auditoriaService } from "../../../services/auditoriaService";
import { formatCreadoEn } from "../../../utils/format";
import Pagination from "../../../components/shared/Pagination";

const PAGE_SIZE = 10;

export default function AuditoriaTab() {
  const [entidad, setEntidad] = useState("");
  const [entidadId, setEntidadId] = useState("");
  const [pagina, setPagina] = useState(0);

  const params = {};
  if (entidad.trim()) params.entidad = entidad.trim();
  if (entidadId.trim()) params.entidadId = Number(entidadId);

  const { data = [], isLoading } = useQuery({
    queryKey: ["auditoria", params],
    queryFn: () => auditoriaService.listar(params),
    staleTime: 30_000,
  });

  const paginados = data.slice(pagina * PAGE_SIZE, (pagina + 1) * PAGE_SIZE);

  return (
    <div className="flex flex-col gap-4">
      <div className="toolbar">
        <div className="search-wrap">
          <input
            className="form-control"
            placeholder="Filtrar por entidad (ej: Pedido, Fondo)"
            value={entidad}
            onChange={(e) => { setEntidad(e.target.value); setPagina(0); }}
          />
        </div>
        <div className="search-wrap">
          <input
            type="number"
            className="form-control"
            placeholder="ID entidad"
            value={entidadId}
            onChange={(e) => { setEntidadId(e.target.value); setPagina(0); }}
          />
        </div>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => { setEntidad(""); setEntidadId(""); setPagina(0); }}
        >
          Limpiar
        </button>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {isLoading ? (
            <div className="loading-center py-10">
              <span className="spinner" />
            </div>
          ) : data.length === 0 ? (
            <div className="empty-state py-10">
              No hay registros de auditoría{entidad || entidadId ? " con los filtros aplicados" : ""}.
            </div>
          ) : (
            <>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha/Hora</th>
                      <th>Acción</th>
                      <th>Entidad</th>
                      <th>ID</th>
                      <th>Detalle</th>
                      <th>Usuario</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginados.map((item) => (
                      <tr key={item.id}>
                        <td className="whitespace-nowrap text-[13px]">
                          {formatCreadoEn(item.registradoEn)}
                        </td>
                        <td>
                          <span className="badge badge-info">{item.accion}</span>
                        </td>
                        <td>{item.entidad}</td>
                        <td>{item.entidadId ?? "—"}</td>
                        <td style={{ maxWidth: "300px", wordBreak: "break-word" }}>
                          {item.detalle}
                        </td>
                        <td>{item.usuario}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {data.length > PAGE_SIZE && (
                <div className="flex justify-end p-3 border-t border-gray-100">
                  <Pagination
                    pagina={pagina}
                    totalItems={data.length}
                    pageSize={PAGE_SIZE}
                    onChange={setPagina}
                  />
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
