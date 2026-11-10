import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { proveedorService } from "../../services/proveedorService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalAdelantoProveedor({ proveedor, onClose }) {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    monto: "", insumo: proveedor.tipo === "CUY" ? "cuy" : "gallina",
    precioUnitarioAcordado: "", fecha: hoy(), observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: adelantos = [], isLoading } = useQuery({
    queryKey: ["proveedor-adelantos", proveedor.id],
    queryFn: () => proveedorService.listarAdelantos(proveedor.id),
  });

  const mutRegistrar = useMutation({
    mutationFn: (data) => proveedorService.registrarAdelanto(proveedor.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["proveedor-adelantos", proveedor.id] });
      qc.invalidateQueries({ queryKey: ["proveedores"] });
      setForm({ monto: "", insumo: form.insumo, precioUnitarioAcordado: "", fecha: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.message),
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.monto || Number(form.monto) <= 0) { setError("El monto debe ser mayor a 0"); return; }
    if (!form.precioUnitarioAcordado || Number(form.precioUnitarioAcordado) <= 0) {
      setError("El precio unitario acordado es obligatorio"); return;
    }
    setSaving(true);
    try {
      await mutRegistrar.mutateAsync({
        monto: Number(form.monto),
        moneda: "COP",
        insumo: form.insumo,
        precioUnitarioAcordado: Number(form.precioUnitarioAcordado),
        fecha: form.fecha,
        observacion: form.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const unidadesDisponibles = (a) =>
    a.saldoRestante > 0 && a.precioUnitarioAcordado > 0
      ? Math.floor(a.saldoRestante / a.precioUnitarioAcordado)
      : 0;

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">Adelantos — {proveedor.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : adelantos.length > 0 ? (
            <div className="table-wrap mb-4">
              <table>
                <thead>
                  <tr>
                    <th>Fecha</th><th>Insumo</th><th>Monto inicial</th>
                    <th>Saldo restante</th><th>Precio u.</th><th>Unidades disp.</th><th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {adelantos.map((a) => (
                    <tr key={a.id}>
                      <td>{a.fecha}</td>
                      <td className="capitalize">{a.insumo}</td>
                      <td>{formatCOP(a.monto)}</td>
                      <td className={a.activo ? "text-amber-700 font-medium" : "text-gray-400"}>
                        {formatCOP(a.saldoRestante)}
                      </td>
                      <td>{formatCOP(a.precioUnitarioAcordado)}</td>
                      <td>{a.activo ? unidadesDisponibles(a) : "—"}</td>
                      <td>
                        <span className={`badge ${a.activo ? "badge-warning" : "badge-success"}`}>
                          {a.activo ? "Activo" : "Agotado"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state mb-4"><p>Sin adelantos registrados</p></div>
          )}

          <form onSubmit={handleSubmit} className="border-t pt-4">
            <p className="text-sm font-medium mb-3">Registrar nuevo adelanto</p>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Monto COP *</label>
                <input
                  type="number" className="form-control" min="1" step="1"
                  value={form.monto}
                  onChange={(e) => setForm((f) => ({ ...f, monto: e.target.value }))}
                  placeholder="2000000"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Insumo *</label>
                <select
                  className="form-control"
                  value={form.insumo}
                  onChange={(e) => setForm((f) => ({ ...f, insumo: e.target.value }))}
                >
                  <option value="gallina">Gallina</option>
                  <option value="cuy">Cuy</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Precio unitario acordado *</label>
                <input
                  type="number" className="form-control" min="1" step="1"
                  value={form.precioUnitarioAcordado}
                  onChange={(e) => setForm((f) => ({ ...f, precioUnitarioAcordado: e.target.value }))}
                  placeholder="47000"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Fecha *</label>
                <input
                  type="date" className="form-control"
                  value={form.fecha}
                  onChange={(e) => setForm((f) => ({ ...f, fecha: e.target.value }))}
                />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Observación</label>
              <input
                className="form-control"
                value={form.observacion}
                onChange={(e) => setForm((f) => ({ ...f, observacion: e.target.value }))}
                placeholder="Opcional"
              />
            </div>
            <div className="flex justify-end mt-2">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? "Registrando…" : "Registrar adelanto"}
              </button>
            </div>
          </form>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}
