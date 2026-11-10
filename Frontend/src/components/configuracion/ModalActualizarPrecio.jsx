import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { formatCOP } from "../../utils/format";

export default function ModalActualizarPrecio({ producto, onClose, onSave }) {
  const [precio, setPrecio] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = async (e) => {
    e.preventDefault();
    const val = parseFloat(precio);
    if (!precio || isNaN(val) || val < 0) {
      setError("Ingrese un precio válido (0 o mayor)");
      return;
    }
    setSaving(true);
    try {
      await onSave({ id: producto.id, precio: val, motivo: null });
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
          <span className="modal-title">Actualizar precio</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <p className="font-semibold text-verde-oscuro mb-1">{producto.nombre}</p>
            <p className="text-sm text-gray-500 mb-4">
              Precio actual:{" "}
              <span className="font-semibold text-gray-700">{formatCOP(producto.precio)}</span>
            </p>
            <div className="form-group mb-0">
              <label className="form-label">Nuevo precio (COP) *</label>
              <input
                className="form-control"
                type="number"
                min="0"
                step="1"
                placeholder="Ej: 25000"
                value={precio}
                onChange={(e) => { setPrecio(e.target.value); setError(""); }}
                autoFocus
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar precio"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
