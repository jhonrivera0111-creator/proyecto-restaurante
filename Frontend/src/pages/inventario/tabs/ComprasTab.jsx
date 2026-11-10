import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Eye, Trash2 } from "lucide-react";
import { compraService } from "../../../services/compraService";
import ModalRegistrarCompra from "../../../components/inventario/ModalRegistrarCompra";
import ModalDetalleCompra from "../../../components/inventario/ModalDetalleCompra";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";
import { formatCOP } from "../../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
};

export default function ComprasTab() {
  const qc = useQueryClient();
  const [fecha, setFecha] = useState(hoy());
  const [estado, setEstado] = useState("");
  const [registrando, setRegistrando] = useState(false);
  const [detalleId, setDetalleId] = useState(null);
  const [anulando, setAnulando] = useState(null);

  const params = {};
  if (fecha) params.fecha = fecha;
  if (estado) params.estado = estado;

  const { data: compras = [], isLoading } = useQuery({
    queryKey: ["compras", params],
    queryFn: () => compraService.listar(params),
  });

  const mutAnular = useMutation({
    mutationFn: compraService.anular,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compras"] });
      setAnulando(null);
    },
  });

  return (
    <div>
      <div className="toolbar">
        <div className="search-wrap flex gap-2 items-center">
          <input
            type="date"
            className="form-control"
            style={{ width: 160 }}
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
          <select
            className="form-control"
            style={{ width: 160 }}
            value={estado}
            onChange={(e) => setEstado(e.target.value)}
          >
            <option value="">Todos los estados</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="ABONADO_PARCIAL">Parcial</option>
            <option value="PAGADO">Pagado</option>
          </select>
        </div>
        <button className="btn btn-primary" onClick={() => setRegistrando(true)}>
          <Plus size={15} /> Registrar compra
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : compras.length === 0 ? (
            <div className="empty-state"><p>Sin compras para los filtros seleccionados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Proveedor</th>
                  <th>Insumo</th>
                  <th>Cantidad</th>
                  <th>Total COP</th>
                  <th>Abonado</th>
                  <th>Saldo</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {compras.map((c) => (
                  <tr key={c.id}>
                    <td>{c.fecha}</td>
                    <td>{c.proveedorNombre}</td>
                    <td className="capitalize">{c.insumo}</td>
                    <td>{c.cantidad}</td>
                    <td>{formatCOP(c.totalCop)}</td>
                    <td>{formatCOP(c.totalAbonado)}</td>
                    <td className={c.saldoPendiente > 0 ? "text-amber-700 font-medium" : ""}>
                      {formatCOP(c.saldoPendiente)}
                    </td>
                    <td>
                      <span className={`badge ${ESTADO_BADGE[c.estado] ?? "badge-gray"}`}>
                        {c.estado}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-1">
                        <button
                          className="btn-icon"
                          title="Ver detalle"
                          onClick={() => setDetalleId(c.id)}
                        >
                          <Eye size={15} />
                        </button>
                        {c.estado === "PENDIENTE" && (
                          <button
                            className="btn-icon"
                            title="Anular"
                            onClick={() => setAnulando(c)}
                          >
                            <Trash2 size={15} className="text-red-500" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {registrando && <ModalRegistrarCompra onClose={() => setRegistrando(false)} />}

      {detalleId && (
        <ModalDetalleCompra compraId={detalleId} onClose={() => setDetalleId(null)} />
      )}

      {anulando && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="Anular compra"
          descripcion={`¿Anular la compra de ${anulando.cantidad} ${anulando.insumo} a ${anulando.proveedorNombre}? El stock se revertirá.`}
          labelOk="Anular"
          peligro
          onOk={() => mutAnular.mutate(anulando.id)}
          onCancelar={() => setAnulando(null)}
        />
      )}
    </div>
  );
}
