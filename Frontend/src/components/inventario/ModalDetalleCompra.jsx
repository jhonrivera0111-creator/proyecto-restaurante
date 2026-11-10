import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { compraService } from "../../services/compraService";
import { fondoService } from "../../services/fondoService";
import { proveedorService } from "../../services/proveedorService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  PAGADO:          "badge-success",
};

export default function ModalDetalleCompra({ compraId, onClose }) {
  const qc = useQueryClient();
  const [abonoForm, setAbonoForm] = useState({ monto: "", moneda: "COP", fecha: hoy(), usarAdelanto: false, adelantoProveedorId: null });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: compra, isLoading } = useQuery({
    queryKey: ["compra", compraId],
    queryFn: () => compraService.obtener(compraId),
  });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const mutAbono = useMutation({
    mutationFn: (data) => compraService.registrarAbono(compraId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["compra", compraId] });
      qc.invalidateQueries({ queryKey: ["compras"] });
      qc.invalidateQueries({ queryKey: ["proveedor-adelantos"] });
      setAbonoForm({ monto: "", moneda: "COP", fecha: hoy(), usarAdelanto: false, adelantoProveedorId: null });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleAbonar = async (e) => {
    e.preventDefault();
    if (abonoForm.usarAdelanto) {
      if (!abonoForm.adelantoProveedorId) { setError("Seleccione el adelanto a usar"); return; }
      setSaving(true);
      try {
        await mutAbono.mutateAsync({ fecha: abonoForm.fecha, adelantoProveedorId: abonoForm.adelantoProveedorId });
      } finally { setSaving(false); }
    } else {
      if (!abonoForm.monto || parseFloat(abonoForm.monto) <= 0) { setError("El monto debe ser mayor a 0"); return; }
      setSaving(true);
      try {
        await mutAbono.mutateAsync({ monto: parseFloat(abonoForm.monto), moneda: abonoForm.moneda, fecha: abonoForm.fecha });
      } finally { setSaving(false); }
    }
  };

  const { data: adelantosActivos = [] } = useQuery({
    queryKey: ["proveedor-adelantos-activos", compra?.proveedorId],
    queryFn: () => proveedorService.listarAdelantos(compra.proveedorId).then(l => l.filter(a => a.activo)),
    enabled: !!compra?.proveedorId,
  });

  const totalAbonado = compra?.abonos?.reduce((s, a) => s + Number(a.montoCop), 0) ?? 0;

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">Detalle de compra</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : compra ? (
            <>
              <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
                <div><span className="text-gray-500">Proveedor:</span> <strong>{compra.proveedorNombre}</strong></div>
                <div><span className="text-gray-500">Insumo:</span> <strong className="capitalize">{compra.insumo}</strong></div>
                <div><span className="text-gray-500">Cantidad:</span> <strong>{compra.cantidad}</strong></div>
                <div><span className="text-gray-500">Precio unitario:</span> <strong>{formatCOP(compra.precioUnitario)}</strong></div>
                <div><span className="text-gray-500">Total:</span> <strong>{formatCOP(compra.totalCop)}</strong></div>
                <div>
                  <span className="text-gray-500">Estado:</span>{" "}
                  <span className={`badge ${ESTADO_BADGE[compra.estado] ?? "badge-gray"}`}>{compra.estado}</span>
                </div>
                {compra.observacion && (
                  <div className="col-span-2"><span className="text-gray-500">Obs:</span> {compra.observacion}</div>
                )}
              </div>

              <div className="flex gap-4 mb-4 text-sm bg-gray-50 rounded p-3">
                <div><span className="text-gray-500">Total abonado:</span> <strong>{formatCOP(compra.totalAbonado)}</strong></div>
                <div><span className="text-gray-500">Saldo pendiente:</span> <strong className="text-amber-700">{formatCOP(compra.saldoPendiente)}</strong></div>
              </div>

              {compra.abonos?.length > 0 && (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Monto</th><th>Moneda</th><th>Total COP</th><th>Fondo</th></tr>
                    </thead>
                    <tbody>
                      {compra.abonos.map((a) => (
                        <tr key={a.id}>
                          <td>{a.fecha}</td>
                          <td>{Number(a.monto).toLocaleString("es-CO")}</td>
                          <td>{a.moneda}</td>
                          <td>{formatCOP(a.montoCop)}</td>
                          <td>{a.fondoDebitado}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colSpan={3} className="text-right font-medium">Total</td>
                        <td className="font-medium">{formatCOP(totalAbonado)}</td>
                        <td />
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}

              {compra.estado !== "PAGADO" && (
                <form onSubmit={handleAbonar} className="border-t pt-4">
                  <p className="text-sm font-medium mb-3">
                    Registrar abono — saldo pendiente: <strong>{formatCOP(compra.saldoPendiente)}</strong>
                  </p>
                  {error && <div className="alert alert-error mb-3">{error}</div>}

                  {adelantosActivos.length > 0 && (
                    <div className="flex items-center gap-3 mb-3 p-2 bg-amber-50 rounded border border-amber-200 text-sm">
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={abonoForm.usarAdelanto}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, usarAdelanto: e.target.checked, adelantoProveedorId: null }))}
                        />
                        Usar adelanto prepagado al proveedor
                      </label>
                    </div>
                  )}

                  {abonoForm.usarAdelanto ? (
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Adelanto a usar *</label>
                        <select
                          className="form-control"
                          value={abonoForm.adelantoProveedorId ?? ""}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, adelantoProveedorId: Number(e.target.value) || null }))}
                        >
                          <option value="">Seleccione…</option>
                          {adelantosActivos.map((a) => (
                            <option key={a.id} value={a.id}>
                              {a.fecha} — saldo {formatCOP(a.saldoRestante)} ({a.insumo})
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Fecha *</label>
                        <input
                          type="date" className="form-control"
                          value={abonoForm.fecha}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, fecha: e.target.value }))}
                        />
                      </div>
                    </div>
                  ) : (
                    <div className="form-row">
                      <div className="form-group">
                        <label className="form-label">Monto *</label>
                        <input
                          type="number" className="form-control" min="1" step="1"
                          value={abonoForm.monto}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, monto: e.target.value }))}
                          placeholder="0"
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Moneda *</label>
                        <select
                          className="form-control"
                          value={abonoForm.moneda}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, moneda: e.target.value }))}
                        >
                          {fondos.map((f) => (
                            <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group">
                        <label className="form-label">Fecha *</label>
                        <input
                          type="date" className="form-control"
                          value={abonoForm.fecha}
                          onChange={(e) => setAbonoForm((f) => ({ ...f, fecha: e.target.value }))}
                        />
                      </div>
                    </div>
                  )}

                  <div className="flex justify-end mt-2">
                    <button type="submit" className="btn btn-primary" disabled={saving}>
                      {saving ? "Registrando…" : "Registrar abono"}
                    </button>
                  </div>
                </form>
              )}
            </>
          ) : null}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}
