import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal para crear/editar una categoría de producto. */
export default function ModalCategoria({ categoria, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: categoria?.nombre ?? "",
    orden:  categoria?.orden  ?? "",
  });
  const [error, setError]   = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    const ordenNum = Number(form.orden);
    if (!form.orden || isNaN(ordenNum) || ordenNum < 1) {
      setError("El orden debe ser un número entero mayor o igual a 1");
      return;
    }
    setSaving(true);
    try {
      await onSave({ nombre: form.nombre.trim(), orden: Math.trunc(ordenNum) });
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
          <span className="modal-title">
            {categoria ? "Editar categoría" : "Nueva categoría"}
          </span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label">Nombre *</label>
              <input
                className="form-control"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Orden *</label>
              <input
                className="form-control"
                type="number"
                min="1"
                step="1"
                value={form.orden}
                onChange={(e) => setForm((f) => ({ ...f, orden: e.target.value }))}
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
