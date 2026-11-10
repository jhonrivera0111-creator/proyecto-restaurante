import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

const TIPOS = ["CUY", "GALLINA", "AMBOS", "OTROS"];

export default function ModalProveedor({ proveedor, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre: proveedor?.nombre ?? "",
    tipo: proveedor?.tipo ?? "GALLINA",
    telefono: proveedor?.telefono ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({
        nombre: form.nombre.trim(),
        tipo: form.tipo,
        telefono: form.telefono.trim() || null,
      });
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
            {proveedor ? "Editar proveedor" : "Nuevo proveedor"}
          </span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group">
              <label className="form-label" htmlFor="proveedor-nombre">Nombre *</label>
              <input
                id="proveedor-nombre"
                className="form-control"
                value={form.nombre}
                onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="proveedor-tipo">Tipo *</label>
              <select
                id="proveedor-tipo"
                className="form-control"
                value={form.tipo}
                onChange={(e) => setForm((f) => ({ ...f, tipo: e.target.value }))}
              >
                {TIPOS.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="proveedor-telefono">Teléfono</label>
              <input
                id="proveedor-telefono"
                className="form-control"
                value={form.telefono}
                onChange={(e) => setForm((f) => ({ ...f, telefono: e.target.value }))}
                placeholder="Opcional"
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
