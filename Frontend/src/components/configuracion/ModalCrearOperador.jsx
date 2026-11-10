import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalCrearOperador({ onClose, onGuardar }) {
  const [nombre, setNombre]   = useState("");
  const [error, setError]     = useState("");
  const [saving, setSaving]   = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onGuardar(nombre.trim());
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message ?? "Error al crear");
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[380px]">
        <div className="modal-header">
          <span className="modal-title">Nuevo operador</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group mb-0">
              <label className="form-label">Nombre</label>
              <input
                className="form-control"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="ej. Carlos"
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Creando…" : "Crear operador"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
