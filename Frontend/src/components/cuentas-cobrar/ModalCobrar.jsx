import { useState } from "react";
import { X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { fondoService } from "../../services/fondoService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalCobrar({ cuenta, onClose, onSave }) {
  const [form, setForm] = useState({ monto: "", moneda: "COP", fecha: hoy(), observacion: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.monto || Number(form.monto) <= 0) { setError("El monto debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({
        monto: Number(form.monto),
        moneda: form.moneda,
        fecha: form.fecha,
        observacion: form.observacion || null,
      });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar cobro — {cuenta.clienteNombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}
          <div className="text-sm bg-gray-50 rounded p-3 mb-4">
            Saldo pendiente: <strong className="text-amber-700">{formatCOP(cuenta.saldoPendiente)}</strong>
          </div>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Monto *</label>
                <input
                  type="number" className="form-control" min="1" step="1"
                  value={form.monto}
                  onChange={(e) => setForm((f) => ({ ...f, monto: e.target.value }))}
                  placeholder="0"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Fondo *</label>
                <select
                  className="form-control"
                  value={form.moneda}
                  onChange={(e) => setForm((f) => ({ ...f, moneda: e.target.value }))}
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
            <div className="modal-footer px-0 pb-0">
              <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? "Registrando…" : "Registrar cobro"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
