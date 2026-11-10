import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

export default function ModalCambiarPassword({ usuario, onClose, onGuardar }) {
  const [nuevaPassword, setNuevaPassword] = useState("");
  const [error, setError]                 = useState("");
  const [saving, setSaving]               = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nuevaPassword || nuevaPassword.length < 4) {
      setError("La contraseña debe tener al menos 4 caracteres");
      return;
    }
    setSaving(true);
    try {
      await onGuardar(nuevaPassword);
      onClose();
    } catch (err) {
      setError(err.response?.data?.mensaje ?? err.message ?? "Error al cambiar contraseña");
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[380px]">
        <div className="modal-header">
          <span className="modal-title">Cambiar contraseña — {usuario.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-group mb-0">
              <label className="form-label">Nueva contraseña (mínimo 4 caracteres)</label>
              <input
                className="form-control"
                type="password"
                value={nuevaPassword}
                onChange={(e) => setNuevaPassword(e.target.value)}
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Cambiar contraseña"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
