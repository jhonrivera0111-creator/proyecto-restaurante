import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalCuentaCobrar({ onClose, onSave }) {
  const [form, setForm] = useState({
    clienteNombre: "", concepto: "", monto: "", celular: "", fecha: hoy(), observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.clienteNombre.trim()) { setError("El nombre del cliente es obligatorio"); return; }
    if (!form.monto || Number(form.monto) <= 0) { setError("El monto debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({
        clienteNombre: form.clienteNombre.trim(),
        concepto: form.concepto || null,
        monto: Number(form.monto),
        celular: form.celular || null,
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
          <span className="modal-title">Registrar deuda</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Cliente *</label>
                <input
                  className="form-control"
                  value={form.clienteNombre}
                  onChange={(e) => setForm((f) => ({ ...f, clienteNombre: e.target.value }))}
                  placeholder="Nombre del cliente"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Celular</label>
                <input
                  className="form-control"
                  value={form.celular}
                  onChange={(e) => setForm((f) => ({ ...f, celular: e.target.value }))}
                  placeholder="3001234567"
                />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Monto COP *</label>
                <input
                  type="number" className="form-control" min="1" step="1"
                  value={form.monto}
                  onChange={(e) => setForm((f) => ({ ...f, monto: e.target.value }))}
                  placeholder="0"
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
              <label className="form-label">Concepto</label>
              <input
                className="form-control"
                value={form.concepto}
                onChange={(e) => setForm((f) => ({ ...f, concepto: e.target.value }))}
                placeholder="Ej: 2 cuyes asados"
              />
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
                {saving ? "Guardando…" : "Registrar deuda"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
