import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalEmpleado({ empleado, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: empleado?.nombre ?? "",
    tipo: empleado?.tipo ?? "QUINCENAL",
    salario: empleado?.salario ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    if (!form.salario || Number(form.salario) <= 0) { setError("El salario debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({ nombre: form.nombre.trim(), tipo: form.tipo, salario: Number(form.salario) });
      onClose();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">
            {empleado ? "Editar empleado" : "Nuevo empleado"}
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
                placeholder="Nombre completo"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Tipo *</label>
              <select
                className="form-control"
                value={form.tipo}
                onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}
              >
                <option value="QUINCENAL">Quincenal (salario fijo)</option>
                <option value="POR_DIA">Por día (tarifa diaria)</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">
                {form.tipo === "QUINCENAL" ? "Salario quincenal (COP) *" : "Tarifa por día (COP) *"}
              </label>
              <input
                type="number"
                className="form-control"
                value={form.salario}
                onChange={(e) => setForm((f) => ({ ...f, salario: e.target.value }))}
                placeholder="0"
                min="1"
                step="1"
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
