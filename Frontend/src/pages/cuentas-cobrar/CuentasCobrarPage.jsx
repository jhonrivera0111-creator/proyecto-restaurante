import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, Trash2 } from "lucide-react";
import { cuentaCobrarService } from "../../services/cuentaCobrarService";
import { pedidoService } from "../../services/pedidoService";
import { productoService } from "../../services/productoService";
import { salonService } from "../../services/salonService";
import { formatCOP } from "../../utils/format";
import { usePedidoMutations } from "../../hooks/usePedidoMutations";
import ModalCuentaCobrar from "../../components/cuentas-cobrar/ModalCuentaCobrar";
import ModalDetalleCuentaCobrar from "../../components/cuentas-cobrar/ModalDetalleCuentaCobrar";
import ConfirmDialog from "../../components/shared/ConfirmDialog";
import OrdenCard from "../../components/pedidos/OrdenCard";
import ModalDetallePedido from "../../components/pedidos/ModalDetallePedido";
import ModalCobrar from "../../components/pedidos/ModalCobrar";

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  CANCELADO:       "badge-success",
};

const ESTADO_LABEL = {
  PENDIENTE: "Pendiente",
  ABONADO_PARCIAL: "Abonado",
  CANCELADO: "Cancelado",
};

export default function CuentasCobrarPage() {
  const qc = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [detalle, setDetalle] = useState(null);
  const [anulando, setAnulando] = useState(null);
  const [filtroEstado, setFiltroEstado] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [buscando, setBuscando] = useState("");
  const [pedidoActivo, setPedidoActivo] = useState(null);
  const [pedidoCobrar, setPedidoCobrar] = useState(null);

  const params = {};
  if (filtroEstado) params.estado = filtroEstado;
  if (buscando) params.q = buscando;

  const { data: cuentas = [], isLoading } = useQuery({
    queryKey: ["cuentas-cobrar", params],
    queryFn: () => cuentaCobrarService.listar(params),
  });

  // Pedidos fiados: mismo queryKey que la pestaña Historial de Pedidos
  // (["pedidos", true]) — comparte caché, no dispara fetch extra si esa
  // pestaña ya se visitó.
  const { data: todosPedidos = [] } = useQuery({
    queryKey: ["pedidos", true],
    queryFn: () => pedidoService.listar(true),
    refetchInterval: 15_000,
  });
  const pedidosFiados = todosPedidos.filter((p) => p.estado === "FIADO");

  const { data: productos = [] } = useQuery({
    queryKey: ["productos-activos"],
    queryFn: () => productoService.listar(true),
  });

  const { data: salones = [] } = useQuery({
    queryKey: ["salones"],
    queryFn: salonService.listar,
  });

  const {
    agregarMut, actualizarDetalleMut, quitarDetalleMut, actualizarMut,
    pagoMut, actualizarPagoMut, quitarPagoMut, finalizarMut, anularMut, fiarMut,
  } = usePedidoMutations();

  const mutRegistrar = useMutation({
    mutationFn: cuentaCobrarService.registrar,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cuentas-cobrar"] }),
  });

  const mutAnular = useMutation({
    mutationFn: cuentaCobrarService.anular,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["cuentas-cobrar"] });
      setAnulando(null);
    },
  });

  const totalPendiente = cuentas
    .filter((c) => c.estado !== "CANCELADO")
    .reduce((s, c) => s + Number(c.saldoPendiente), 0);
  const cuentasAbiertas = cuentas.filter((c) => c.estado !== "CANCELADO").length;
  const cobradoMes = cuentas.reduce((s, c) => s + Number(c.totalAbonado), 0);

  return (
    <div className="page-content">
      <div className="page-header">
        <h1 className="page-title">Cuentas por Cobrar</h1>
        <button className="btn btn-primary" onClick={() => setCreando(true)}>
          <Plus size={15} /> Nueva deuda
        </button>
      </div>

      <div className="kpi-grid mb-4">
        <div className="kpi-card">
          <div className="kpi-label">Total pendiente</div>
          <div className="kpi-value text-amber-700">{formatCOP(totalPendiente)}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Cuentas abiertas</div>
          <div className="kpi-value">{cuentasAbiertas}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Cobrado (período)</div>
          <div className="kpi-value text-green-700">{formatCOP(cobradoMes)}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Pedidos fiados</div>
          <div className="kpi-value text-amber-700">{pedidosFiados.length}</div>
        </div>
      </div>

      {pedidosFiados.length > 0 && (
        <div className="card mb-4">
          <div className="card-header">
            <h2 className="card-title">Pedidos fiados · {pedidosFiados.length}</h2>
          </div>
          <div className="card-body">
            <div className="grid gap-[14px]" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))" }}>
              {pedidosFiados.map((p, idx) => (
                <OrdenCard key={p.id} pedido={p} idx={idx}
                  onEditar={setPedidoActivo}
                  onCobrar={setPedidoCobrar}
                />
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="toolbar mb-4">
        <div className="search-wrap">
          <Search size={14} />
          <input
            className="form-control"
            placeholder="Buscar cliente…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && setBuscando(busqueda)}
          />
        </div>
        <div className="flex gap-2">
          {["", "PENDIENTE", "ABONADO_PARCIAL", "CANCELADO"].map((e) => (
            <button
              key={e}
              className={`btn btn-sm ${filtroEstado === e ? "btn-primary" : "btn-secondary"}`}
              onClick={() => setFiltroEstado(e)}
            >
              {e === "" ? "Todos" : ESTADO_LABEL[e]}
            </button>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : cuentas.length === 0 ? (
            <div className="empty-state"><p>Sin cuentas por cobrar</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Cliente</th><th>Concepto</th><th>Fecha</th>
                  <th>Deuda</th><th>Pendiente</th><th>Estado</th><th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {cuentas.map((c) => (
                  <tr key={c.id} className="cursor-pointer hover:bg-gray-50" onClick={() => setDetalle(c.id)}>
                    <td className="font-medium">{c.clienteNombre}</td>
                    <td className="text-sm text-gray-500">{c.concepto ?? "—"}</td>
                    <td>{c.fecha}</td>
                    <td>{formatCOP(c.monto)}</td>
                    <td className={c.saldoPendiente > 0 ? "text-amber-700 font-medium" : "text-gray-400"}>
                      {formatCOP(c.saldoPendiente)}
                    </td>
                    <td>
                      <span className={`badge ${ESTADO_BADGE[c.estado] ?? "badge-gray"}`}>
                        {ESTADO_LABEL[c.estado] ?? c.estado}
                      </span>
                    </td>
                    <td onClick={(e) => e.stopPropagation()}>
                      {c.estado === "PENDIENTE" && (
                        <button
                          className="btn-icon"
                          title="Anular"
                          onClick={() => setAnulando(c)}
                        >
                          <Trash2 size={14} className="text-red-500" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {creando && (
        <ModalCuentaCobrar
          onClose={() => setCreando(false)}
          onSave={mutRegistrar.mutateAsync}
        />
      )}

      {detalle && (
        <ModalDetalleCuentaCobrar
          cuentaId={detalle}
          onClose={() => setDetalle(null)}
        />
      )}

      {anulando && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="Anular cuenta"
          descripcion={`¿Anular la deuda de ${anulando.clienteNombre} por ${formatCOP(anulando.monto)}? Esta acción no se puede deshacer.`}
          labelOk="Anular"
          peligro
          onOk={() => mutAnular.mutate(anulando.id)}
          onCancelar={() => setAnulando(null)}
        />
      )}

      {pedidoActivo && (
        <ModalDetallePedido
          pedido={pedidoActivo}
          productos={productos}
          salones={salones}
          onClose={() => setPedidoActivo(null)}
          onActualizar={(req) => actualizarMut.mutateAsync({ id: pedidoActivo.id, req }).then(setPedidoActivo)}
          onAgregarDetalle={(req) => agregarMut.mutateAsync({ id: pedidoActivo.id, req }).then(setPedidoActivo)}
          onActualizarDetalle={(detalleId, req) => actualizarDetalleMut.mutateAsync({ id: pedidoActivo.id, detalleId, req }).then(setPedidoActivo)}
          onQuitarDetalle={(detalleId) => quitarDetalleMut.mutateAsync({ id: pedidoActivo.id, detalleId }).then(setPedidoActivo)}
          onFinalizar={() => finalizarMut.mutateAsync(pedidoActivo.id).then(() => setPedidoActivo(null))}
          onAnular={() => anularMut.mutateAsync(pedidoActivo.id).then(() => setPedidoActivo(null))}
          onFiar={() => fiarMut.mutateAsync(pedidoActivo.id).then(() => setPedidoActivo(null))}
          onActualizarPago={(pagoId, req) => actualizarPagoMut.mutateAsync({ id: pedidoActivo.id, pagoId, req }).then(setPedidoActivo)}
          onQuitarPago={(pagoId) => quitarPagoMut.mutateAsync({ id: pedidoActivo.id, pagoId }).then(setPedidoActivo)}
        />
      )}

      {pedidoCobrar && (
        <ModalCobrar
          pedido={pedidoCobrar}
          onClose={() => setPedidoCobrar(null)}
          onPago={(req) => pagoMut.mutateAsync({ id: pedidoCobrar.id, req }).then(setPedidoCobrar)}
          onFinalizar={() => finalizarMut.mutateAsync(pedidoCobrar.id).then(() => setPedidoCobrar(null))}
          onFiar={() => fiarMut.mutateAsync(pedidoCobrar.id).then(() => setPedidoCobrar(null))}
        />
      )}
    </div>
  );
}
